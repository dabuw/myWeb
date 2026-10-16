package com.portfolio.portfolio.controller;

import com.portfolio.portfolio.dto.ChangePasswordRequest;
import com.portfolio.portfolio.dto.PortfolioResponse;
import com.portfolio.portfolio.model.Profile;
import com.portfolio.portfolio.model.ProjectLink;
import com.portfolio.portfolio.service.AdminAuthService;
import com.portfolio.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${app.cors.allowed-origin:http://localhost:5173}")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final AdminAuthService adminAuthService;

    public PortfolioController(PortfolioService portfolioService, AdminAuthService adminAuthService) {
        this.portfolioService = portfolioService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping("/public/portfolio")
    public PortfolioResponse getPortfolio() {
        return portfolioService.getPortfolio();
    }

    @GetMapping("/admin/auth-check")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void authCheck(@RequestHeader(value = "X-Admin-Password", required = false) String adminPassword) {
        adminAuthService.verifyOrThrow(adminPassword);
    }

    @PostMapping("/admin/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @RequestHeader(value = "X-Admin-Password", required = false) String adminPassword,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        adminAuthService.verifyOrThrow(adminPassword);
        adminAuthService.changePassword(request.getOldPassword(), request.getNewPassword());
    }

    @PutMapping("/admin/profile")
    public Profile updateProfile(
            @RequestHeader(value = "X-Admin-Password", required = false) String adminPassword,
            @Valid @RequestBody Profile profile
    ) {
        adminAuthService.verifyOrThrow(adminPassword);
        return portfolioService.saveProfile(profile);
    }

    @PostMapping("/admin/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectLink addProject(
            @RequestHeader(value = "X-Admin-Password", required = false) String adminPassword,
            @Valid @RequestBody ProjectLink projectLink
    ) {
        adminAuthService.verifyOrThrow(adminPassword);
        return portfolioService.addProject(projectLink);
    }

    @PutMapping("/admin/projects/{id}")
    public ProjectLink updateProject(
            @RequestHeader(value = "X-Admin-Password", required = false) String adminPassword,
            @PathVariable Long id,
            @Valid @RequestBody ProjectLink projectLink
    ) {
        adminAuthService.verifyOrThrow(adminPassword);
        return portfolioService.updateProject(id, projectLink);
    }

    @DeleteMapping("/admin/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(
            @RequestHeader(value = "X-Admin-Password", required = false) String adminPassword,
            @PathVariable Long id
    ) {
        adminAuthService.verifyOrThrow(adminPassword);
        portfolioService.deleteProject(id);
    }
}
