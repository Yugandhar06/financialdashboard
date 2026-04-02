package com.finance.dashboard.controller;

import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.dto.response.MonthlyTrendEntry;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

/**
 * DashboardController — analytics and summary endpoints.
 *
 * ACCESS CONTROL:
 * - /recent → all roles (VIEWER gets this — useful activity feed)
 * - All others → ANALYST and ADMIN only (summary/analytics are elevated access)
 *
 * DESIGN: All computation happens in DashboardService.
 * This controller only routes the request and formats the response.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard/summary
     *
     * Returns the complete dashboard overview in one call:
     * - Total income across all time
     * - Total expenses across all time
     * - Net balance (income - expenses)
     * - Category-wise expense breakdown
     * - 10 most recent transactions
     *
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "totalIncome": 45000.00,
     *     "totalExpenses": 28500.00,
     *     "netBalance": 16500.00,
     *     "categoryTotals": { "Rent": 9000.00, "Food": 4800.00, "Transport": 1200.00 },
     *     "recentTransactions": [ ...10 records... ]
     *   }
     * }
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard summary retrieved", dashboardService.getSummary()));
    }

    /**
     * GET /api/dashboard/category-totals?type=EXPENSE
     *
     * Returns spending breakdown by category for a given transaction type.
     * Defaults to EXPENSE if type is not specified.
     *
     * Useful for pie charts showing where money is going.
     *
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "Rent": 9000.00,
     *     "Food": 4800.00,
     *     "Utilities": 1800.00
     *   }
     * }
     */
    @GetMapping("/category-totals")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getCategoryTotals(
            @RequestParam(required = false) TransactionType type) {

        return ResponseEntity.ok(
                ApiResponse.success("Category totals retrieved",
                        dashboardService.getCategoryTotals(type)));
    }

    /**
     * GET /api/dashboard/monthly-trends?months=12
     *
     * Returns month-by-month income and expense data for trend charts.
     * Default: last 12 months. Max: 36 months (to prevent heavy queries).
     *
     * Response:
     * {
     *   "success": true,
     *   "data": [
     *     { "year": 2024, "month": 1, "income": 5000.00, "expenses": 3200.00, "net": 1800.00 },
     *     { "year": 2024, "month": 2, "income": 5000.00, "expenses": 2900.00, "net": 2100.00 },
     *     ...
     *   ]
     * }
     */
    @GetMapping("/monthly-trends")
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<MonthlyTrendEntry>>> getMonthlyTrends(
            @RequestParam(defaultValue = "12") int months) {

        // Cap at 36 months to prevent unnecessarily heavy queries
        int safeMonths = Math.min(Math.max(months, 1), 36);

        return ResponseEntity.ok(
                ApiResponse.success("Monthly trends retrieved",
                        dashboardService.getMonthlyTrends(safeMonths)));
    }
    								
    /**
     * GET /api/dashboard/recent?limit=10
     *
     * Returns the most recent transactions across the system.
     * Available to ALL roles — even Viewers can see recent activity.
     *
     * Response:
     * {
     *   "success": true,
     *   "data": [ ...last 10 transactions, newest first... ]
     * }
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                ApiResponse.success("Recent transactions retrieved",
                        dashboardService.getRecentTransactions(limit)));
    }
}