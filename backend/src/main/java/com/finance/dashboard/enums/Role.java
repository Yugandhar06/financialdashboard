package com.finance.dashboard.enums;

/**
 * Defines the three access levels in the system.
 *
 * VIEWER  → Can only read transaction data and recent activity
 * ANALYST → Can read + access dashboard analytics/summaries
 * ADMIN   → Full access: create, update, delete records + manage users
 *
 * These are stored as strings in MySQL (not integers) so the DB
 * remains readable without needing a lookup table.
 */
public enum Role {
    VIEWER,
    ANALYST,
    ADMIN
}