package com.finance.dashboard.controller;

import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.AnalystDashboardResponse;
import com.finance.dashboard.dto.response.InsightsResponse;
import com.finance.dashboard.service.DashboardService;
import com.finance.dashboard.service.InsightsService;
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
    private final InsightsService insightsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<AnalystDashboardResponse>> getAnalystDashboard(Principal principal) {
        String email = principal.getName();
        return ResponseEntity.ok(
                ApiResponse.success("Analyst dashboard retrieved",
                        dashboardService.getAnalystDashboard(email)));
    }

    /**
     * Advanced analytics and insights for the current user's transactions.
     * Provides spending trends, category analysis, budget status, and actionable recommendations.
     * 
     * Available to: ANALYST, ADMIN
     * 
     * @return InsightsResponse containing:
     *         - Spending trends (this month vs last month with projections)
     *         - Category analysis (top categories, breakdowns, trends)
     *         - Budget status (vs $5000/month budget with visual status)
     *         - Recommendations (suggestions, warnings, opportunities)
     */
    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<InsightsResponse>> getInsights(Principal principal) {
        String email = principal.getName();
        InsightsResponse insights = insightsService.generateInsights(email);
        return ResponseEntity.ok(
                ApiResponse.success("Advanced insights generated",
                        insights));
    }
}
