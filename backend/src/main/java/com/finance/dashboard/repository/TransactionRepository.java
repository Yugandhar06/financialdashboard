package com.finance.dashboard.repository;

import com.finance.dashboard.entity.Transaction;
import com.finance.dashboard.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * TransactionRepository — data access for the `transactions` table.
 *
 * Two key additions beyond basic JpaRepository:
 *
 * 1. JpaSpecificationExecutor<Transaction>
 *    → Enables dynamic query building (Specifications).
 *    → Used by findAll(Specification, Pageable) for flexible filtering
 *      (date range + category + type in any combination).
 *
 * 2. Custom @Query methods for dashboard aggregations.
 *    → We use DB-level SUM/GROUP BY instead of loading all rows into Java.
 *    → Critical for performance: a finance app may have millions of records.
 *
 * ALL queries include: WHERE t.isDeleted = false
 * This is the "soft delete filter" — deleted records are invisible everywhere.
 */
@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    // ── Soft-Delete Aware Finder ─────────────────────────────────────────

    /**
     * Find a non-deleted transaction by ID.
     * Used instead of findById() everywhere to respect soft delete.
     */
    Optional<Transaction> findByIdAndIsDeletedFalse(Long id);

    /**
     * Paginated list of all non-deleted transactions.
     * Used when no filters are applied.
     */
    Page<Transaction> findByIsDeletedFalse(Pageable pageable);

    // ── Dashboard Aggregation Queries ────────────────────────────────────

    /**
     * Total income or expenses across all non-deleted transactions.
     *
     * Uses COALESCE to return 0.00 instead of NULL when there are no rows.
     * (SUM of empty set = NULL in SQL — COALESCE prevents NPE in Java)
     *
     * Example: getTotalByType(INCOME) → 45000.00
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t " +
           "WHERE t.type = :type AND t.isDeleted = false")
    BigDecimal getTotalByType(@Param("type") TransactionType type);

    /**
     * Category-wise totals for a given transaction type.
     * Returns List<Object[]> where each element is [categoryName, total].
     *
     * Example result for EXPENSE:
     *   ["Food", 1200.00]
     *   ["Rent", 3000.00]
     *   ["Transport", 450.00]
     *
     * The service converts this raw result into a Map<String, BigDecimal>.
     */
    @Query("SELECT t.category, COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t " +
           "WHERE t.type = :type AND t.isDeleted = false " +
           "GROUP BY t.category " +
           "ORDER BY SUM(t.amount) DESC")
    List<Object[]> getCategoryTotals(@Param("type") TransactionType type);

    /**
     * Monthly income/expense totals for the past N months.
     * Groups by year and month for time-series chart data.
     *
     * Returns List<Object[]>: [year, month, type, total]
     *
     * Example:
     *   [2024, 1, "INCOME",  5000.00]
     *   [2024, 1, "EXPENSE", 3200.00]
     *   [2024, 2, "INCOME",  5500.00]
     *   ...
     */
    @Query("SELECT YEAR(t.date), MONTH(t.date), t.type, COALESCE(SUM(t.amount), 0) " +
           "FROM Transaction t " +
           "WHERE t.isDeleted = false AND t.date >= :fromDate " +
           "GROUP BY YEAR(t.date), MONTH(t.date), t.type " +
           "ORDER BY YEAR(t.date) ASC, MONTH(t.date) ASC")
    List<Object[]> getMonthlyTrends(@Param("fromDate") LocalDate fromDate);

    /**
     * Most recent transactions for the dashboard activity feed.
     * Pageable controls how many (default: 10).
     */
    @Query("SELECT t FROM Transaction t " +
           "WHERE t.isDeleted = false " +
           "ORDER BY t.date DESC, t.createdAt DESC")
    List<Transaction> findRecentTransactions(Pageable pageable);
}