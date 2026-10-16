package com.portfolio.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理接口登录失败限流。
 *
 * <p>规则：同一个来源 IP，在一个时间窗口内最多允许失败 {@code loginMaxAttempts} 次。
 * 窗口从「第一次失败」开始计时，到期后计数自动清零。
 * 一旦登录成功立即清零，所以正常使用不会被误伤。
 *
 * <p>为什么要做这个：仓库是公开的，{@code /api/admin/auth-check} 这个路径
 * 以及它用 {@code X-Admin-Password} 传密码的方式全世界都能读到，
 * 而接口本身没有失败次数限制，等于给暴力破解留了门。
 *
 * <p>计数保存在内存里——单实例部署够用。重启后端会清空计数，
 * 也就是说运维可以随时把自己从误锁里放出来（攻击者做不到）。
 */
@Component
public class AdminLoginRateLimiter {

    /** 超过这个条目数就顺手清理过期记录，防止大量不同来源 IP 把内存撑爆 */
    private static final int PURGE_THRESHOLD = 10_000;

    private final int maxAttempts;
    private final Duration window;

    private record Attempts(int count, Instant windowStart) {
    }

    private final Map<String, Attempts> attemptsByIp = new ConcurrentHashMap<>();

    public AdminLoginRateLimiter(
            @Value("${app.admin.login-max-attempts:5}") int maxAttempts,
            @Value("${app.admin.login-window-minutes:60}") long windowMinutes
    ) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    /** 已被限流就抛 429（带剩余等待分钟数），否则放行。 */
    public void checkAllowed(String clientIp) {
        Attempts attempts = attemptsByIp.get(clientIp);
        if (attempts == null || isExpired(attempts)) {
            return;
        }
        if (attempts.count() >= maxAttempts) {
            long waitMinutes = minutesUntilExpiry(attempts);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "登录尝试次数过多，请在 " + waitMinutes + " 分钟后重试"
            );
        }
    }

    /** 记录一次失败；窗口内第一次失败时开始计时。 */
    public void recordFailure(String clientIp) {
        purgeIfCrowded();
        attemptsByIp.compute(clientIp, (ip, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new Attempts(1, Instant.now());
            }
            return new Attempts(existing.count() + 1, existing.windowStart());
        });
    }

    /** 登录成功，清空该 IP 的失败记录。 */
    public void recordSuccess(String clientIp) {
        attemptsByIp.remove(clientIp);
    }

    private boolean isExpired(Attempts attempts) {
        return Instant.now().isAfter(attempts.windowStart().plus(window));
    }

    private long minutesUntilExpiry(Attempts attempts) {
        long minutes = Duration.between(Instant.now(), attempts.windowStart().plus(window)).toMinutes();
        return Math.max(1, minutes + 1);
    }

    private void purgeIfCrowded() {
        if (attemptsByIp.size() < PURGE_THRESHOLD) {
            return;
        }
        attemptsByIp.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
}
