package com.finance.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * InsightsResponse — comprehensive analytics and spending insights.
 *
 * Shows: trends, predictions, budget status, recommendations.
 * Designed for analysis dashboard and decision-making.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsightsResponse {

    // Spending trends and prediction
    private SpendingTrend spendingTrend;

    // Category analysis
    private CategoryAnalysis categoryAnalysis;

    // Budget vs actual tracking
    private BudgetStatus budgetStatus;

    // AI-generated recommendations
    private Recommendations recommendations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpendingTrend {
        private BigDecimal thisMonth;      // Current month spending
        private BigDecimal lastMonth;      // Previous month spending
        private String trendDirection;     // "UP", "DOWN", "STABLE"
        private String trendPercentage;    // "+15.2%", "-8.5%"
        private BigDecimal predictedNextMonth;  // ML prediction
        private String insight;            // Human-readable insight
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryAnalysis {
        private String topCategory;        // Highest spending category
        private BigDecimal topCategoryAmount;
        private String topCategoryTrend;   // "+15%", "-8%"
        private String topCategoryInsight; // e.g., "Your Food spending increased 15% vs last month"
        
        private String lowestCategory;     // Lowest spending category
        private BigDecimal lowestCategoryAmount;
        
        private Map<String, CategoryDetail> categoryBreakdown;  // All categories with details
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryDetail {
        private String name;
        private BigDecimal totalSpent;
        private BigDecimal percentOfTotal;
        private String trend;              // "+5%", "-10%"
        private String status;             // "normal", "high", "low"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BudgetStatus {
        private Map<String, BudgetItem> categories;
        private BigDecimal totalBudget;
        private BigDecimal totalSpent;
        private BigDecimal totalRemaining;
        private String overallStatus;      // "on-track", "warning", "exceeded"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BudgetItem {
        private String category;
        private BigDecimal assigned;       // Budget limit
        private BigDecimal spent;          // Actual spending
        private BigDecimal remaining;      // Remaining budget
        private String percentUsed;        // "75.5%"
        private String status;             // "green" (< 75%), "yellow" (75-90%), "red" (> 90%)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Recommendations {
        private String[] suggestions;      // Array of actionable recommendations
        private String[] warnings;         // Array of warning messages
        private String[] opportunities;    // Array of saving opportunities
    }
}
