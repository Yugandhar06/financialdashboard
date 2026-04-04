package com.finance.dashboard.dto.response;

import com.finance.dashboard.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AuditLogResponse — safe response object for audit logs.
 * Excludes sensitive internal data, exposes only what's needed for audit viewing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {

    private Long id;
    private Long entityId;
    private String entityType;       // "TRANSACTION", "USER"
    private String action;            // "CREATE", "UPDATE", "DELETE"
    private String changedBy;         // Who made the change
    private String fieldName;         // Which field changed (for UPDATE)
    private String oldValue;          // Previous value
    private String newValue;          // New value
    private LocalDateTime timestamp;  // When it changed

    /**
     * Convert AuditLog entity to response DTO
     */
    public static AuditLogResponse fromEntity(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .entityId(log.getEntityId())
                .entityType(log.getEntityType())
                .action(log.getAction())
                .changedBy(log.getChangedBy())
                .fieldName(log.getFieldName())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .timestamp(log.getTimestamp())
                .build();
    }
}
