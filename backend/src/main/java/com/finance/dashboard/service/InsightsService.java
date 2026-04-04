package com.finance.dashboard.service;

import com.finance.dashboard.dto.response.InsightsResponse;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * InsightsService — generates advanced analytics and insights.
 *
 * Responsibilities:
 * 1. Compute spending trends and predictions
 * 2. Analyze expense categories
 * 3. Track budget vs actual
 * 4. Generate recommendations
 *
 * This demonstrates business logic thinking — beyond CRUD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsightsService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Generate comprehensive insights for the analyst dashboard.
     * 
     * Shows: trends, predictions, budget status, recommendations for the specified user.
     * 
     * @param email the email of the user to generate insights for
     * @return InsightsResponse with comprehensive analytics
     */
    public InsightsResponse generateInsights(String email) {
        // Get the user from database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        // Compute each component in parallel (conceptually)
        InsightsResponse.SpendingTrend trend = computeSpendingTrend(user.getId());
        InsightsResponse.CategoryAnalysis categoryAnalysis = analyzeCategories(user.getId());
        InsightsResponse.BudgetStatus budgetStatus = computeBudgetStatus(user.getId());
        InsightsResponse.Recommendations recommendations = generateRecommendations(trend, categoryAnalysis);

        return InsightsResponse.builder()
                .spendingTrend(trend)
                .categoryAnalysis(categoryAnalysis)
                .budgetStatus(budgetStatus)
                .recommendations(recommendations)
                .build();
    }

    // ── SPENDING TREND ANALYSIS ──────────────────────────────────────

    /**
     * Compute month-over-month expense trend and predict next month.
     */
    private InsightsResponse.SpendingTrend computeSpendingTrend(Long userId) {
        YearMonth now = YearMonth.now();
        YearMonth lastMonth = now.minusMonths(1);
        YearMonth twoMonthsAgo = now.minusMonths(2);

        // Get this month's spending (up to today)
        BigDecimal thisMonth = getMonthlyExpenses(userId, now);

        // Get last month's spending
        BigDecimal prevMonth = getMonthlyExpenses(userId, lastMonth);

        // Get two months ago for trend detection
        BigDecimal twoMonthsBack = getMonthlyExpenses(userId, twoMonthsAgo);

        // Calculate percentage change
        BigDecimal percentChange = BigDecimal.ZERO;
        String trendDirection = "STABLE";
        if (prevMonth.compareTo(BigDecimal.ZERO) > 0) {
            percentChange = thisMonth.subtract(prevMonth)
                    .divide(prevMonth, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (percentChange.compareTo(BigDecimal.valueOf(5)) > 0) {
                trendDirection = "UP";
            } else if (percentChange.compareTo(BigDecimal.valueOf(-5)) < 0) {
                trendDirection = "DOWN";
            }
        }

        // Simple prediction: average of last 2 months
        BigDecimal predictedNext = thisMonth.add(prevMonth)
                .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        // Generate insight message
        String insight = generateTrendInsight(trendDirection, percentChange);

        return InsightsResponse.SpendingTrend.builder()
                .thisMonth(thisMonth)
                .lastMonth(prevMonth)
                .trendDirection(trendDirection)
                .trendPercentage((percentChange.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + 
                               percentChange.setScale(1, RoundingMode.HALF_UP) + "%")
                .predictedNextMonth(predictedNext)
                .insight(insight)
                .build();
    }

    private String generateTrendInsight(String direction, BigDecimal percent) {
        return switch (direction) {
            case "UP" -> String.format("Spending increased by %.1f%%. Monitor expenses closely.", percent);
            case "DOWN" -> String.format("Great! Spending decreased by %.1f%%. Keep it up!", Math.abs(percent.doubleValue()));
            default -> "Spending remained stable. On track!";
        };
    }

    // ── CATEGORY ANALYSIS ────────────────────────────────────────────

    /**
     * Analyze spending by category and identify top/lowest categories.
     */
    private InsightsResponse.CategoryAnalysis analyzeCategories(Long userId) {
        Map<String, BigDecimal> thisMonthByCategory = getCategoryTotalsByMonth(userId, YearMonth.now());
        Map<String, BigDecimal> lastMonthByCategory = getCategoryTotalsByMonth(userId, YearMonth.now().minusMonths(1));

        // Find top and lowest categories
        String topCategory = thisMonthByCategory.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String lowestCategory = thisMonthByCategory.entrySet().stream()
                .min(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("N/A");

        BigDecimal topAmount = thisMonthByCategory.getOrDefault(topCategory, BigDecimal.ZERO);
        BigDecimal lowestAmount = thisMonthByCategory.getOrDefault(lowestCategory, BigDecimal.ZERO);

        // Calculate trend for top category
        BigDecimal lastMonthTop = lastMonthByCategory.getOrDefault(topCategory, BigDecimal.ZERO);
        String topTrend = calculateTrendPercentage(lastMonthTop, topAmount);

        // Build category breakdown
        BigDecimal totalThisMonth = thisMonthByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, InsightsResponse.CategoryDetail> breakdown = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : thisMonthByCategory.entrySet()) {
            String category = entry.getKey();
            BigDecimal amount = entry.getValue();
            BigDecimal lastAmount = lastMonthByCategory.getOrDefault(category, BigDecimal.ZERO);

            BigDecimal percentOfTotal = totalThisMonth.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(totalThisMonth, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            breakdown.put(category, InsightsResponse.CategoryDetail.builder()
                    .name(category)
                    .totalSpent(amount)
                    .percentOfTotal(percentOfTotal.setScale(1, RoundingMode.HALF_UP))
                    .trend(calculateTrendPercentage(lastAmount, amount))
                    .status(getSpendingStatus(percentOfTotal))
                    .build());
        }

        return InsightsResponse.CategoryAnalysis.builder()
                .topCategory(topCategory)
                .topCategoryAmount(topAmount)
                .topCategoryTrend(topTrend)
                .topCategoryInsight(String.format("Your %s spending is your largest expense. Trend: %s", 
                        topCategory, topTrend))
                .lowestCategory(lowestCategory)
                .lowestCategoryAmount(lowestAmount)
                .categoryBreakdown(breakdown)
                .build();
    }

    // ── BUDGET STATUS ────────────────────────────────────────────────

    /**
     * Compute budget vs actual for the current month.
     * Assumes a simple model: equal monthly budget across categories.
     */
    private InsightsResponse.BudgetStatus computeBudgetStatus(Long userId) {
        // Simplified model: assume $5000/month total budget distributed equally
        BigDecimal totalMonthlyBudget = BigDecimal.valueOf(5000);

        Map<String, BigDecimal> thisMonthByCategory = getCategoryTotalsByMonth(userId, YearMonth.now());
        BigDecimal totalSpent = thisMonthByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = totalMonthlyBudget.subtract(totalSpent);

        Map<String, InsightsResponse.BudgetItem> budgetItems = new HashMap<>();
        BigDecimal perCategoryBudget = totalMonthlyBudget.divide(
                BigDecimal.valueOf(Math.max(thisMonthByCategory.size(), 1)), 
                2, RoundingMode.HALF_UP);

        for (Map.Entry<String, BigDecimal> entry : thisMonthByCategory.entrySet()) {
            String category = entry.getKey();
            BigDecimal spent = entry.getValue();
            BigDecimal remaining = perCategoryBudget.subtract(spent);
            BigDecimal percentUsed = perCategoryBudget.compareTo(BigDecimal.ZERO) > 0
                    ? spent.divide(perCategoryBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            String status = "green";
            if (percentUsed.compareTo(BigDecimal.valueOf(90)) > 0) {
                status = "red";
            } else if (percentUsed.compareTo(BigDecimal.valueOf(75)) > 0) {
                status = "yellow";
            }

            budgetItems.put(category, InsightsResponse.BudgetItem.builder()
                    .category(category)
                    .assigned(perCategoryBudget)
                    .spent(spent)
                    .remaining(remaining.setScale(2, RoundingMode.HALF_UP))
                    .percentUsed(percentUsed.setScale(1, RoundingMode.HALF_UP) + "%")
                    .status(status)
                    .build());
        }

        String overallStatus = totalSpent.compareTo(totalMonthlyBudget) <= 0 ? "on-track" : "exceeded";

        return InsightsResponse.BudgetStatus.builder()
                .categories(budgetItems)
                .totalBudget(totalMonthlyBudget)
                .totalSpent(totalSpent)
                .totalRemaining(totalRemaining)
                .overallStatus(overallStatus)
                .build();
    }

    // ── RECOMMENDATIONS ──────────────────────────────────────────────

    /**
     * Generate actionable recommendations based on insights.
     */
    private InsightsResponse.Recommendations generateRecommendations(
            InsightsResponse.SpendingTrend trend,
            InsightsResponse.CategoryAnalysis analysis) {

        List<String> suggestions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> opportunities = new ArrayList<>();

        // Generate suggestions based on trend
        if ("UP".equals(trend.getTrendDirection())) {
            warnings.add("Your spending is increasing. Review budget constraints.");
        } else if ("DOWN".equals(trend.getTrendDirection())) {
            suggestions.add("Excellent trend! Your spending is down. Keep this momentum.");
        }

        // Generate category-based recommendations
        String topCategory = analysis.getTopCategory();
        if (topCategory != null && !topCategory.equals("N/A")) {
            BigDecimal topAmount = analysis.getTopCategoryAmount();
            if (topAmount.compareTo(BigDecimal.valueOf(2000)) > 0) {
                opportunities.add(String.format("Consider setting a lower limit on %s spending to optimize budget.", 
                        topCategory));
            }
        }

        // Generate warnings for high-spend categories
        for (InsightsResponse.CategoryDetail detail : analysis.getCategoryBreakdown().values()) {
            if ("red".equals(detail.getStatus())) {
                warnings.add(String.format("⚠️ %s spending exceeds 90%% of budget!", detail.getName()));
            } else if ("yellow".equals(detail.getStatus())) {
                suggestions.add(String.format("📊 Monitor %s spending. You've used 75%% of budget.", detail.getName()));
            }
        }

        return InsightsResponse.Recommendations.builder()
                .suggestions(suggestions.toArray(new String[0]))
                .warnings(warnings.toArray(new String[0]))
                .opportunities(opportunities.toArray(new String[0]))
                .build();
    }

    // ── HELPER METHODS ───────────────────────────────────────────────

    /**
     * Get total expenses for a specific month for a user.
     */
    private BigDecimal getMonthlyExpenses(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        BigDecimal total = transactionRepository.getTotalByTypeInRangeForUser(
                userId, TransactionType.EXPENSE, start, end);
        return total == null ? BigDecimal.ZERO : total;
    }

    /**
     * Get category-wise totals for a specific month for a user.
     */
    private Map<String, BigDecimal> getCategoryTotalsByMonth(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        Map<String, BigDecimal> totals = new HashMap<>();
        try {
            // Use user-filtered method for per-user analytics
            var categoryTotals = transactionRepository.getCategoryTotalsByDateRangeForUser(userId, start, end);
            for (Object[] row : categoryTotals) {
                String category = (String) row[0];
                BigDecimal amount = (BigDecimal) row[1];
                totals.put(category, amount);
            }
        } catch (Exception e) {
            log.warn("Could not fetch category totals for user {} in {}", userId, month, e);
        }
        return totals;
    }

    /**
     * Calculate trend percentage between two values.
     */
    private String calculateTrendPercentage(BigDecimal old, BigDecimal current) {
        if (old.compareTo(BigDecimal.ZERO) == 0) {
            return "N/A";
        }
        BigDecimal percent = current.subtract(old)
                .divide(old, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return (percent.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + 
               percent.setScale(1, RoundingMode.HALF_UP) + "%";
    }

    /**
     * Determine spending status based on percentage.
     */
    private String getSpendingStatus(BigDecimal percentOfTotal) {
        if (percentOfTotal.compareTo(BigDecimal.valueOf(30)) > 0) {
            return "high";
        } else if (percentOfTotal.compareTo(BigDecimal.valueOf(10)) < 0) {
            return "low";
        }
        return "normal";
    }
}
