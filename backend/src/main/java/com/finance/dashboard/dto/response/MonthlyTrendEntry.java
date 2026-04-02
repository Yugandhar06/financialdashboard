package com.finance.dashboard.dto.response;

import lombok.*;
import java.math.BigDecimal;

/**
 * MonthlyTrendEntry — one data point in the monthly trend chart.
 *
 * Example:
 * { "year": 2024, "month": 3, "income": 5000.00, "expenses": 3200.00, "net": 1800.00 }
 *
 * The frontend uses this array to render a line/bar chart.
 * We return net = income - expenses so the frontend doesn't need to compute it.
 */
@Getter
@Builder
@AllArgsConstructor
public class MonthlyTrendEntry {
    private int year;
    private int month;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal net;       // Precomputed: income - expenses for this month
}