package com.sky.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.blog.common.BizException;
import com.sky.blog.common.ErrorCode;
import com.sky.blog.common.TimeUtil;
import com.sky.blog.dto.AdminDto;
import com.sky.blog.dto.AuthDto;
import com.sky.blog.entity.User;
import com.sky.blog.enums.AuditAction;
import com.sky.blog.enums.AuditTarget;
import com.sky.blog.enums.Role;
import com.sky.blog.enums.UserStatus;
import com.sky.blog.mapper.UserMapper;
import com.sky.blog.security.LoginUser;
import com.sky.blog.security.Principal;
import com.sky.blog.vo.PageVo;
import com.sky.blog.vo.UserVo;
import jakarta.servlet.http.HttpSession;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户域：注册、登录、资料、改密码，以及后台的用户管理。
 *
 * <p>作业 6.2 要求指明"登录鉴权、权限判断"的实现位置：
 * 拦截层在 {@code security/AuthInterceptor}，账号规则在本类。
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    // ------------------------------------------------------------ 注册 / 登录

    /**
     * 注册。
     *
     * <p>注意入参里<b>没有</b> role 字段，角色一律服务端写 USER —— 满足作业 2.3.2
     * "普通注册流程只能创建普通用户，不能通过提交角色字段注册管理员"。
     */
    @Transactional(rollbackFor = Exception.class)
    public UserVo.Detail register(AuthDto.Register dto) {
        String username = dto.username().trim();
        String email = normalizeEmail(dto.email());

        if (exists(User::getUsername, username)) {
            throw new BizException(ErrorCode.DUPLICATE, "该用户名已被注册");
        }
        if (exists(User::getEmail, email)) {
            throw new BizException(ErrorCode.DUPLICATE, "该邮箱已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setNickname(blankTo(dto.nickname(), username));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setCredentialVersion(0);
        user.setDeleted(0);
        userMapper.insert(user);
        log.info("新用户注册 id={} username={} role=USER", user.getId(), user.getUsername());
        return UserVo.Detail.of(user);
    }

    /**
     * 登录：用户名或邮箱都可以。
     *
     * <p>失败原因不区分"用户不存在"和"密码错误"，统一回"用户名或密码错误"，避免账号枚举。
     * <b>唯一例外</b>是密码正确但账号被禁用 —— 这时才提示"账号已被禁用"，
     * 因为作业 2.2.1 要求给出明确提示，而能走到这一步说明对方已经掌握了正确密码。
     */
    @Transactional(readOnly = true)
    public UserVo.Detail login(AuthDto.Login dto, HttpSession session) {
        String account = dto.account().trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, account).or().eq(User::getEmail, normalizeEmail(account)))
                .last("LIMIT 1"));

        if (user == null || !passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            log.warn("登录失败 account={}", account);
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 登录成功后只放 userId 和凭据版本号；角色每次请求回库取最新值
        session.setAttribute(LoginUser.SESSION_KEY,
                LoginUser.of(user.getId(), user.getCredentialVersion()));
        log.info("登录成功 id={} username={}", user.getId(), user.getUsername());
        return UserVo.Detail.of(user);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    // ------------------------------------------------------------ 个人中心

    @Transactional(readOnly = true)
    public UserVo.Detail detail(Long userId) {
        return UserVo.Detail.of(mustFind(userId));
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVo.Detail updateProfile(Long userId, AuthDto.ProfileUpdate dto) {
        User user = mustFind(userId);
        user.setNickname(dto.nickname().trim());
        user.setAvatar(trimToNull(dto.avatar()));
        user.setBio(trimToNull(dto.bio()));
        user.setUpdatedAt(TimeUtil.now());
        userMapper.updateById(user);
        return UserVo.Detail.of(user);
    }

    /**
     * 改密码。
     *
     * <p>作业 2.2.1 要求"明确修改后登录状态的处理规则"，这里的规则是：
     * 改密成功后 {@code credentialVersion + 1}，并立刻让当前会话失效 ——
     * 旧会话即使还带着 Cookie，也会被 {@code AuthInterceptor} 用版本号不匹配挡掉（401）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, AuthDto.PasswordUpdate dto, HttpSession session) {
        User user = mustFind(userId);
        if (!passwordEncoder.matches(dto.oldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "原密码不正确");
        }
        if (dto.oldPassword().equals(dto.newPassword())) {
            throw new BizException(ErrorCode.UNPROCESSABLE, "新密码不能与原密码相同");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        user.setCredentialVersion((user.getCredentialVersion() == null ? 0 : user.getCredentialVersion()) + 1);
        user.setUpdatedAt(TimeUtil.now());
        userMapper.updateById(user);
        session.invalidate();
        log.info("用户改密 id={} credentialVersion={}", userId, user.getCredentialVersion());
    }

    // ------------------------------------------------------------ 后台用户管理

    @Transactional(readOnly = true)
    public PageVo<UserVo.Detail> adminPage(long current, long size, String keyword,
                                           Role role, UserStatus status) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            q.and(w -> w.like(User::getUsername, kw).or().like(User::getEmail, kw)
                    .or().like(User::getNickname, kw));
        }
        if (role != null) {
            q.eq(User::getRole, role);
        }
        if (status != null) {
            q.eq(User::getStatus, status);
        }
        q.orderByAsc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(current, size), q);
        return PageVo.of(page, UserVo.Detail::of);
    }

    /** 禁用 / 恢复账号 */
    @Transactional(rollbackFor = Exception.class)
    public UserVo.Detail adminSetStatus(Principal operator, Long targetId, AdminDto.UserStatusUpdate dto) {
        User target = mustFind(targetId);
        UserStatus next = parseStatus(dto.status());

        if (next == UserStatus.DISABLED) {
            // 顺序有讲究：先判"会不会导致后台无人可管"，再判"是不是在动自己"。
            // 反过来的话，唯一管理员点"禁用自己"只会看到一句含糊的"不能禁用自己的账号"。
            assertNotLastActiveAdmin(target, "禁用");
            if (operator.userId().equals(targetId)) {
                throw BizException.unprocessable("不能禁用自己的账号");
            }
        }
        target.setStatus(next);
        target.setUpdatedAt(TimeUtil.now());
        userMapper.updateById(target);

        auditLogService.record(operator,
                next == UserStatus.DISABLED ? AuditAction.USER_DISABLE : AuditAction.USER_ENABLE,
                AuditTarget.USER, targetId, target.getUsername(), dto.remark());
        return UserVo.Detail.of(target);
    }

    /** 修改角色 */
    @Transactional(rollbackFor = Exception.class)
    public UserVo.Detail adminSetRole(Principal operator, Long targetId, AdminDto.UserRoleUpdate dto) {
        User target = mustFind(targetId);
        Role next = parseRole(dto.role());
        if (target.getRole() == next) {
            throw BizException.unprocessable("该用户已经是" + next.name() + "角色");
        }
        if (next == Role.USER) {
            assertNotLastActiveAdmin(target, "降级");
            if (operator.userId().equals(targetId)) {
                throw BizException.unprocessable("不能降级自己的管理员角色");
            }
        }
        Role before = target.getRole();
        target.setRole(next);
        target.setUpdatedAt(TimeUtil.now());
        userMapper.updateById(target);

        auditLogService.record(operator, AuditAction.USER_ROLE_CHANGE, AuditTarget.USER,
                targetId, target.getUsername(),
                "角色 " + before + " -> " + next + (dto.remark() == null ? "" : "；" + dto.remark()));
        return UserVo.Detail.of(target);
    }

    // ------------------------------------------------------------ 公共小工具

    @Transactional(readOnly = true)
    public User mustFind(Long userId) {
        User user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw BizException.notFound();
        }
        return user;
    }

    /** 批量取作者信息，给列表用，避免 N+1 查询 */
    @Transactional(readOnly = true)
    public Map<Long, UserVo.Author> authorsByIds(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(User::getId, UserVo.Author::of, (a, b) -> a, LinkedHashMap::new));
    }

    @Transactional(readOnly = true)
    public Map<Long, User> usersByIds(Collection<Long> ids) {
        List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(distinct).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * 作业 2.3.2 的硬规则：系统必须保留至少一个启用状态的管理员账号，防止误操作导致后台无人可管。
     */
    private void assertNotLastActiveAdmin(User target, String verb) {
        boolean wasProtected = target.getRole() == Role.ADMIN && target.getStatus() == UserStatus.ACTIVE;
        if (!wasProtected) {
            return;
        }
        long others = userMapper.countOtherActiveAdmins(target.getId());
        if (others == 0) {
            throw BizException.unprocessable("系统必须保留至少一个启用状态的管理员，无法" + verb + "该账号");
        }
    }

    private boolean exists(com.baomidou.mybatisplus.core.toolkit.support.SFunction<User, ?> column, Object value) {
        return userMapper.exists(new LambdaQueryWrapper<User>().eq(column, value));
    }

    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public static Role parseRole(String value) {
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "角色只能是 USER 或 ADMIN");
        }
    }

    public static UserStatus parseStatus(String value) {
        try {
            return UserStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "状态只能是 ACTIVE 或 DISABLED");
        }
    }

    private static String trimToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private static String blankTo(String v, String fallback) {
        String t = trimToNull(v);
        return t == null ? fallback : t;
    }
}
