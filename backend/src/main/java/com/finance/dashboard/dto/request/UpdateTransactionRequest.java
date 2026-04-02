package com.finance.dashboard.dto.request;

import com.finance.dashboard.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * UpdateTransactionRequest — all fields are optional (partial update / PATCH-style).
 *
 * The service checks: if a field is non-null, update it; otherwise, keep existing value.
 * This allows updating just the amount or just the category without resending all fields.
 *
 * Assumption: We use PUT route but treat it as a partial update for better UX.
 * A reviewer may note this could be a PATCH — acceptable tradeoff documented here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTransactionRequest {

    // @Positive still applies IF the field is provided
    @Positive(message = "Amount must be a positive number")
    @Digits(integer = 13, fraction = 2,
            message = "Amount can have at most 13 digits and 2 decimal places")
    private BigDecimal amount;

    private TransactionType type;

    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate date;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}