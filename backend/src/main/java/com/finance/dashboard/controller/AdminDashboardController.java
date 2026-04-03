package com.finance.dashboard.controller;

import com.finance.dashboard.dto.response.AdminDashboardResponse;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard(Principal principal) {
        String email = principal.getName();
        return ResponseEntity.ok(
                ApiResponse.success("Admin dashboard retrieved",
                        dashboardService.getAdminDashboard(email)));
    }
}
