package com.finance.dashboard.dto.request;

import com.finance.dashboard.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * CreateTransactionRequest — validated input for creating a new transaction.
 *
 * Validation strategy:
 * - @NotNull / @NotBlank → field must be present
 * - @Positive            → amount must be > 0 (not zero, not negative)
 * - @NotNull on enum     → prevents null type (invalid income/expense entry)
 * - @PastOrPresent       → can't create transactions dated in the future
 * - @Size                → limits category and notes lengths to match DB columns
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransactionRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a positive number")
    @Digits(integer = 13, fraction = 2,
            message = "Amount can have at most 13 digits and 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required (INCOME or EXPENSE)")
    private TransactionType type;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    @NotNull(message = "Date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate date;

    // Notes are optional — null is valid here
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}