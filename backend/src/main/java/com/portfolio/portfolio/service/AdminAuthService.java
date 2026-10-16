package com.portfolio.portfolio.service;

import com.portfolio.portfolio.model.AdminUser;
import com.portfolio.portfolio.repository.AdminUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final AdminLoginRateLimiter rateLimiter;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminUsername;

    public AdminAuthService(AdminUserRepository adminUserRepository, AdminLoginRateLimiter rateLimiter) {
        this.adminUserRepository = adminUserRepository;
        this.rateLimiter = rateLimiter;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.adminUsername = "admin";
    }

    public void verifyOrThrow(String providedPassword) {
        String clientIp = currentClientIp();
        rateLimiter.checkAllowed(clientIp);

        if (providedPassword == null || providedPassword.isBlank()) {
            rateLimiter.recordFailure(clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请填写管理密码");
        }

        AdminUser adminUser = adminUserRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "管理员账号未初始化"));

        if (!passwordEncoder.matches(providedPassword, adminUser.getPasswordHash())) {
            rateLimiter.recordFailure(clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "管理密码错误");
        }

        rateLimiter.recordSuccess(clientIp);
    }

    /**
     * 取真实客户端 IP，用于限流计数。
     *
     * <p>只信任我们自己的 Nginx 写入的头：容器里的 Nginx 用
     * {@code proxy_set_header X-Real-IP $remote_addr} 覆盖写这个头，
     * 客户端自己伪造的 X-Real-IP 会被覆盖掉。
     * X-Forwarded-For 只作兜底，并且取<b>最后一段</b>——因为 Nginx 用的是
     * {@code $proxy_add_x_forwarded_for}（追加而非覆盖），第一段客户端可以随便填。
     */
    private String currentClientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return "unknown";
        }
        HttpServletRequest request = servletAttributes.getRequest();

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            return parts[parts.length - 1].trim();
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }

    public void changePassword(String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写旧密码");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写新密码");
        }
        if (newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码长度至少为 8 位");
        }
        if (oldPassword.equals(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与旧密码相同");
        }

        AdminUser adminUser = adminUserRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "管理员账号未初始化"));

        if (!passwordEncoder.matches(oldPassword, adminUser.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "旧密码不正确");
        }

        adminUser.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserRepository.save(adminUser);
    }
}
