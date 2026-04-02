package com.finance.dashboard.enums;

/**
 * A transaction is either money coming in (INCOME) or money going out (EXPENSE).
 * Stored as a string enum in MySQL for clarity.
 */
public enum TransactionType {
    INCOME,
    EXPENSE
}