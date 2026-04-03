package com.finance.dashboard.controller;

import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.AnalystDashboardResponse;
import com.finance.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/analyst")
@RequiredArgsConstructor
public class AnalystController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<AnalystDashboardResponse>> getAnalystDashboard(Principal principal) {
        String email = principal.getName();
        return ResponseEntity.ok(
                ApiResponse.success("Analyst dashboard retrieved",
                        dashboardService.getAnalystDashboard(email)));
    }
}
