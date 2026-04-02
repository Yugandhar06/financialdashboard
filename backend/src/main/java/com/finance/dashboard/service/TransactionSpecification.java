package com.finance.dashboard.service;

import com.finance.dashboard.entity.Transaction;
import com.finance.dashboard.enums.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TransactionSpecification — builds dynamic JPA queries for filtering transactions.
 *
 * WHY SPECIFICATIONS?
 * Filtering can have many combinations:
 *   - Just by type
 *   - Just by date range
 *   - By type + category
 *   - By all three filters at once
 *   - No filters (return all)
 *
 * Without Specifications, you'd need a separate repository method for every
 * possible filter combination — that's an explosion of methods.
 *
 * With Specifications, we build the WHERE clause dynamically at runtime.
 * Only the filters that are actually provided (non-null) are added.
 *
 * RESULTING SQL (example — all filters applied):
 *   SELECT * FROM transactions
 *   WHERE is_deleted = false
 *     AND type = 'EXPENSE'
 *     AND category = 'Food'
 *     AND date >= '2024-01-01'
 *     AND date <= '2024-03-31'
 *   ORDER BY date DESC
 */
public class TransactionSpecification {

    /**
     * Returns a Specification that applies all non-null filters.
     *
     * @param type      filter by INCOME or EXPENSE (null = no filter)
     * @param category  filter by category string (null = no filter)
     * @param startDate filter transactions on or after this date (null = no filter)
     * @param endDate   filter transactions on or before this date (null = no filter)
     */
    public static Specification<Transaction> withFilters(
            TransactionType type,
            String category,
            LocalDate startDate,
            LocalDate endDate) {

        return (root, query, criteriaBuilder) -> {
            // Collect all active predicates into a list
            List<Predicate> predicates = new ArrayList<>();

            // ── Always-on filter: exclude soft-deleted records ──────────
            predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));

            // ── Optional filters (only added if parameter is non-null) ──

            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            if (category != null && !category.isBlank()) {
                // Case-insensitive LIKE match: "food" matches "Food", "FOOD"
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("category")),
                        "%" + category.toLowerCase().trim() + "%"
                ));
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("date"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("date"), endDate));
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}