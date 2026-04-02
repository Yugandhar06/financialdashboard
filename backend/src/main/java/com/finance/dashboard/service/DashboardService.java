package com.finance.dashboard.service;

import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.dto.response.MonthlyTrendEntry;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DashboardService — computes all analytics and summary data.
 *
 * PERFORMANCE DESIGN:
 * All aggregations (SUM, GROUP BY) are done at the DATABASE level.
 * We never load all transaction rows into Java memory to compute totals.
 * For a finance app with millions of rows, this is critical.
 *
 * DB computes:   SUM, GROUP BY, ORDER BY
 * Java computes: net balance (income - expenses), structuring the response
 *
 * All calculations use BigDecimal to preserve exact decimal arithmetic.
 * (Float/Double would introduce rounding errors in financial totals)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // Read-only transaction — optimizes DB for reads
public class DashboardService {

    private final TransactionRepository transactionRepository;

    // ── Summary: Total Income, Expenses, Net Balance ─────────────────────

    /**
     * Returns the top-level financial summary:
     * - totalIncome    → SUM of all INCOME transactions
     * - totalExpenses  → SUM of all EXPENSE transactions
     * - netBalance     → totalIncome - totalExpenses
     * - categoryTotals → category-wise expense breakdown
     * - recentTransactions → last 10 records for the activity feed
     *
     * All figures are for ALL TIME (no date range filter here).
     * Date-filtered summaries can be added via query params in future.
     */
    public DashboardSummaryResponse getSummary() {
        // These two DB calls each execute a single SQL SUM query
        BigDecimal totalIncome   = transactionRepository.getTotalByType(TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.getTotalByType(TransactionType.EXPENSE);

        // Net balance is computed in Java — simple subtraction of two DB values
        BigDecimal netBalance = totalIncome.subtract(totalExpenses);

        // Category totals for expense breakdown (most useful for dashboards)
        Map<String, BigDecimal> categoryTotals = getCategoryTotalsMap(TransactionType.EXPENSE);

        // Recent transactions for the activity feed (last 10)
        List<TransactionResponse> recent = transactionRepository
                .findRecentTransactions(PageRequest.of(0, 10))
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .categoryTotals(categoryTotals)
                .recentTransactions(recent)
                .build();
    }

    // ── Category-Wise Totals ─────────────────────────────────────────────

    /**
     * Returns category totals for a given transaction type.
     *
     * Useful for pie/bar charts showing expense breakdown.
     *
     * Example output:
     * {
     *   "Rent":      3000.00,
     *   "Food":      1200.00,
     *   "Transport":  450.00
     * }
     *
     * @param type INCOME or EXPENSE (defaults to EXPENSE if null)
     */
    public Map<String, BigDecimal> getCategoryTotals(TransactionType type) {
        // Default to EXPENSE if no type specified (most common dashboard use case)
        TransactionType queryType = (type != null) ? type : TransactionType.EXPENSE;
        return getCategoryTotalsMap(queryType);
    }

    // ── Monthly Trends ───────────────────────────────────────────────────

    /**
     * Returns month-by-month income and expense totals for the past N months.
     * Used to render line/bar trend charts on the dashboard.
     *
     * ALGORITHM:
     * 1. Query DB for raw [year, month, type, total] rows
     * 2. Group the raw rows by (year, month) in Java
     * 3. Build MonthlyTrendEntry with income + expense totals per month
     * 4. Fill in 0.00 for months where one type had no transactions
     *
     * @param months number of past months to include (default: 12)
     * @return list of monthly data points, sorted oldest → newest
     */
    public List<MonthlyTrendEntry> getMonthlyTrends(int months) {
        // Calculate the cutoff date: first day of month N months ago
        LocalDate fromDate = LocalDate.now()
                .minusMonths(months)
                .withDayOfMonth(1);

        // Raw result: List of [year(int), month(int), type(String), total(BigDecimal)]
        List<Object[]> rawData = transactionRepository.getMonthlyTrends(fromDate);

        // Group by (year, month) key — each group will have up to 2 rows (INCOME, EXPENSE)
        // Key format: "2024-01", "2024-02", etc. (ensures correct sort order)
        Map<String, MonthlyTrendAccumulator> monthMap = new LinkedHashMap<>();

        for (Object[] row : rawData) {
            int year          = ((Number) row[0]).intValue();
            int month         = ((Number) row[1]).intValue();
            String typeStr    = row[2].toString();
            BigDecimal total  = (BigDecimal) row[3];

            // Create a sortable key: "2024-01", "2024-12"
            String key = String.format("%04d-%02d", year, month);

            // Get or create the accumulator for this month
            monthMap.putIfAbsent(key, new MonthlyTrendAccumulator(year, month));
            MonthlyTrendAccumulator acc = monthMap.get(key);

            // Assign the total to the correct field based on transaction type
            if (TransactionType.INCOME.name().equals(typeStr)) {
                acc.income = total;
            } else {
                acc.expenses = total;
            }
        }

        // Convert accumulators to final MonthlyTrendEntry DTOs
        return monthMap.values().stream()
                .map(acc -> MonthlyTrendEntry.builder()
                        .year(acc.year)
                        .month(acc.month)
                        .income(acc.income)
                        .expenses(acc.expenses)
                        .net(acc.income.subtract(acc.expenses))
                        .build())
                .collect(Collectors.toList());
    }

    // ── Recent Transactions ──────────────────────────────────────────────

    /**
     * Returns the N most recent transactions.
     * Used for the "Recent Activity" widget on the dashboard.
     *
     * @param limit number of records to return (max 50 to prevent abuse)
     */
    public List<TransactionResponse> getRecentTransactions(int limit) {
        int safeLimit = Math.min(limit, 50);  // cap to prevent large responses
        return transactionRepository
                .findRecentTransactions(PageRequest.of(0, safeLimit))
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Converts the raw Object[] result from the repository into a typed Map.
     * Raw row format: [categoryName (String), total (BigDecimal)]
     */
    private Map<String, BigDecimal> getCategoryTotalsMap(TransactionType type) {
        List<Object[]> rawResult = transactionRepository.getCategoryTotals(type);

        // Convert raw rows to LinkedHashMap (preserves the ORDER BY total DESC from DB)
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rawResult) {
            String category  = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            result.put(category, total);
        }
        return result;
    }

    /**
     * Internal accumulator used to build MonthlyTrendEntry.
     * Holds running income and expense totals for a single month.
     * ZERO defaults ensure months with only income or only expense work correctly.
     */
    private static class MonthlyTrendAccumulator {
        int year;
        int month;
        BigDecimal income   = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        MonthlyTrendAccumulator(int year, int month) {
            this.year  = year;
            this.month = month;
        }
    }
}