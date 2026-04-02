package com.finance.dashboard.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DashboardSummaryResponse — returned by GET /api/dashboard/summary
 *
 * Contains the key financial figures:
 * - totalIncome, totalExpenses → raw aggregations
 * - netBalance = totalIncome - totalExpenses (calculated in service, not DB)
 * - categoryTotals → Map of { "Food": 1200.00, "Rent": 3000.00, ... }
 * - recentTransactions → last N records for quick display
 *
 * BigDecimal is used everywhere for money — no floating point.
 */
@Getter
@Builder
@AllArgsConstructor
public class DashboardSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;           // Computed: income - expenses
    private Map<String, BigDecimal> categoryTotals;
    private List<TransactionResponse> recentTransactions;
}