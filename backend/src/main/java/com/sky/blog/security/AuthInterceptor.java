package com.sky.blog.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.sky.blog.common.BizException;
import com.sky.blog.common.ErrorCode;
import com.sky.blog.entity.User;
import com.sky.blog.enums.UserStatus;
import com.sky.blog.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权 + 角色判断（作业 6.2 要求指明"登录鉴权、权限判断"的关键逻辑位置，就是本类）。
 *
 * <p>每个受保护请求都做四步检查，顺序不能换：
 * <ol>
 *   <li>会话里没有登录信息 → 401 请先登录；</li>
 *   <li>回库查用户，查不到（被删）→ 401 登录状态已失效；</li>
 *   <li>账号被禁用 → 401 + 明确提示，并清掉会话
 *       —— 这一步保证作业 2.2.1 的"已有登录状态也不能继续访问受保护数据或执行写操作"；</li>
 *   <li>credential_version 与登录时不一致（改过密码）→ 401 密码已修改，请重新登录。</li>
 * </ol>
 *
 * <p>角色<b>不信任会话里的快照</b>，每次都取库里的最新值，所以管理员被降级后立即生效。
 * 代价是每次请求一条主键查询；SQLite 单主键查询是微秒级，在这个规模下可以忽略。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final UserMapper userMapper;

    public AuthInterceptor(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HandlerMethod method = (handler instanceof HandlerMethod hm) ? hm : null;
        RequireLogin requireLogin = method == null ? null : findAnnotation(method, RequireLogin.class);
        RequireRole requireRole = method == null ? null : findAnnotation(method, RequireRole.class);

        // 先尽量解析登录者：公开接口也要能知道"当前是不是登录用户"（详情页的点赞/收藏状态）
        Principal principal = resolveQuietly(request);

        if (requireLogin != null && principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (requireRole != null) {
            if (principal == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            if (principal.role() != requireRole.value()) {
                log.warn("越权访问被拦截：{} 需要 {}，实际 {}", request.getRequestURI(),
                        requireRole.value(), principal.role());
                throw new BizException(ErrorCode.FORBIDDEN);
            }
        }
        if (principal != null) {
            Principal.Holder.set(principal);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Principal.Holder.clear();
    }

    /**
     * 解析会话并回库校验；不满足条件时返回 null（由调用方决定是 401 还是当游客处理）。
     *
     * <p>注意：这里主动清会话的场景（禁用/删除/改密码），是"退出后不得继续执行需要登录的操作"
     * 在服务端的兜底实现。
     */
    private Principal resolveQuietly(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object raw = session.getAttribute(LoginUser.SESSION_KEY);
        if (!(raw instanceof LoginUser login)) {
            return null;
        }
        User user = userMapper.selectById(login.userId());
        if (user == null) {
            session.invalidate();
            throw new BizException(ErrorCode.SESSION_EXPIRED);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            session.invalidate();
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!login.credentialMatches(user.getCredentialVersion())) {
            session.invalidate();
            throw new BizException(ErrorCode.PASSWORD_CHANGED);
        }
        return new Principal(user.getId(), user.getUsername(), user.getRole());
    }

    /** 方法上的注解优先，其次看类上的注解。 */
    private static <A extends java.lang.annotation.Annotation> A findAnnotation(
            HandlerMethod method, Class<A> type) {
        A onMethod = method.getMethodAnnotation(type);
        return onMethod != null ? onMethod : method.getBeanType().getAnnotation(type);
    }
}
