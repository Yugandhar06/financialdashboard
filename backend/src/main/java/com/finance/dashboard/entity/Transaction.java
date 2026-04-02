package com.finance.dashboard.entity;

import com.finance.dashboard.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transaction Entity — maps to the `transactions` table in MySQL.
 *
 * Design notes:
 * - amount uses BigDecimal (DECIMAL in MySQL) — NEVER Float/Double for money
 *   because floating-point arithmetic causes precision errors (e.g., 0.1 + 0.2 ≠ 0.3)
 * - `date` is the business date (when the transaction occurred)
 *   `created_at` is the system date (when the record was entered)
 *   These can differ: e.g., entering last month's expense today
 * - `isDeleted` implements soft delete — records are never physically removed
 *   This preserves audit history and supports future recovery features
 * - Indexed columns (type, category, date) allow fast filtering in dashboard queries
 */
@Entity
@Table(name = "transactions", indexes = {
        // These indexes match the most common dashboard/filter query patterns
        @Index(name = "idx_type",     columnList = "type"),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_date",     columnList = "date"),
        @Index(name = "idx_deleted",  columnList = "is_deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who created this transaction — FK relationship to users table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // DECIMAL(15, 2) in MySQL → supports up to 999,999,999,999,999.99
    // BigDecimal in Java → exact arithmetic, no floating-point errors
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    // Free-form category string (e.g., "Salary", "Rent", "Food")
    // Could be normalized to a Category table in a larger system
    @Column(nullable = false, length = 100)
    private String category;

    // Business date — when the transaction actually occurred
    @Column(nullable = false)
    private LocalDate date;

    // Optional description/memo for the transaction
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Soft delete flag — filter in every query with: WHERE is_deleted = false
    // Never hard-delete financial records (audit trail requirement)
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}