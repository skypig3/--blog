package com.sky.blog.security;

import com.sky.blog.common.BizException;
import com.sky.blog.common.ErrorCode;
import com.sky.blog.enums.Role;

/**
 * 当前请求的登录者。由 {@link AuthInterceptor} 在 preHandle 里写入，afterCompletion 里清理。
 *
 * <p>Controller 从这里取 userId；<b>Service 层不读 ThreadLocal</b>，一律靠方法参数传进去，
 * 这样 Service 可以直接写单测，不用伪造请求上下文。
 */
public record Principal(Long userId, String username, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public static final class Holder {

        private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

        private Holder() {
        }

        public static void set(Principal principal) {
            CURRENT.set(principal);
        }

        /** 公开接口取不到登录者时返回 null（例如详情页要判断"我是否点过赞"）。 */
        public static Principal current() {
            return CURRENT.get();
        }

        public static void clear() {
            CURRENT.remove();
        }

        /** 一定要拿到人时用这个，拿不到就 401。 */
        public static Principal require() {
            Principal p = CURRENT.get();
            if (p == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            return p;
        }

        public static Long requireUserId() {
            return require().userId();
        }
    }
}
