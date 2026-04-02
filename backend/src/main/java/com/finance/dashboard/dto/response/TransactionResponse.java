package com.finance.dashboard.dto.response;

import com.finance.dashboard.entity.Transaction;
import com.finance.dashboard.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TransactionResponse — the public shape of a transaction.
 *
 * WHY a separate DTO instead of returning the entity directly?
 *
 * 1. The entity has fields we don't want to expose (is_deleted, user object)
 * 2. The entity is tied to JPA — returning it directly creates coupling
 * 3. We can add/remove fields in the response without changing the DB schema
 * 4. Prevents accidental exposure of sensitive data
 *
 * The static fromEntity() method is a clean factory that converts
 * a Transaction entity → TransactionResponse DTO in one place.
 */
@Getter
@Builder
@AllArgsConstructor
public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private String category;
    private LocalDate date;
    private String notes;
    private String createdBy;     // User's name (not full user object)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Converts a Transaction entity to a TransactionResponse DTO.
     * Single place of conversion — easier to maintain.
     */
    public static TransactionResponse fromEntity(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType())
                .category(t.getCategory())
                .date(t.getDate())
                .notes(t.getNotes())
                .createdBy(t.getUser() != null ? t.getUser().getName() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}