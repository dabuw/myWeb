package com.portfolio.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService {

    private final String adminPassword;

    public AdminAuthService(@Value("${app.admin.password}") String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public void verifyOrThrow(String providedPassword) {
        if (providedPassword == null || providedPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing admin password");
        }
        if (!adminPassword.equals(providedPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin password");
        }
    }
}
