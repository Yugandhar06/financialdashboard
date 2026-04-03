package com.finance.dashboard.controller;

import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.ViewerDashboardResponse;
import com.finance.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/viewer")
@RequiredArgsConstructor
public class ViewerController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<ViewerDashboardResponse>> getViewerDashboard(Principal principal) {
        String email = principal.getName();
        return ResponseEntity.ok(
                ApiResponse.success("Viewer dashboard retrieved",
                        dashboardService.getViewerDashboard(email)));
    }
}
