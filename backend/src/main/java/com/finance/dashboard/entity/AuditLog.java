package com.finance.dashboard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AuditLog Entity — tracks all changes to financial records.
 *
 * Purpose: Compliance, security, and audit trail.
 * Records WHO changed WHAT, WHEN, and TO WHAT VALUES.
 *
 * Design notes:
 * - Immutable after creation (@Setter not used, only @CreationTimestamp)
 * - Stores both old and new values for full change history
 * - entityType allows tracking changes to different entity types
 * - Never soft-deleted; audit logs are immutable
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_entity_id", columnList = "entity_id"),
        @Index(name = "idx_entity_type", columnList = "entity_type"),
        @Index(name = "idx_action", columnList = "action"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which entity was changed
    @Column(nullable = false)
    private Long entityId;

    // Type of entity: "TRANSACTION", "USER"
    @Column(nullable = false, length = 50)
    private String entityType;

    // Action performed: "CREATE", "UPDATE", "DELETE"
    @Column(nullable = false, length = 20)
    private String action;

    // Who made the change
    @Column(nullable = false, length = 150)
    private String changedBy;

    // Field that was changed (null for CREATE/DELETE)
    @Column(length = 100)
    private String fieldName;

    // Old value (null for CREATE)
    @Column(columnDefinition = "TEXT")
    private String oldValue;

    // New value (null for DELETE)
    @Column(columnDefinition = "TEXT")
    private String newValue;

    // System timestamp
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * Helper method to create an audit log for CREATE action
     */
    public static AuditLog createLog(Long entityId, String entityType, String changedBy, String newValue) {
        return AuditLog.builder()
                .entityId(entityId)
                .entityType(entityType)
                .action("CREATE")
                .changedBy(changedBy)
                .newValue(newValue)
                .build();
    }

    /**
     * Helper method to create an audit log for UPDATE action
     */
    public static AuditLog updateLog(Long entityId, String entityType, String changedBy,
                                     String fieldName, String oldValue, String newValue) {
        return AuditLog.builder()
                .entityId(entityId)
                .entityType(entityType)
                .action("UPDATE")
                .changedBy(changedBy)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
    }

    /**
     * Helper method to create an audit log for DELETE action
     */
    public static AuditLog deleteLog(Long entityId, String entityType, String changedBy, String deletedValue) {
        return AuditLog.builder()
                .entityId(entityId)
                .entityType(entityType)
                .action("DELETE")
                .changedBy(changedBy)
                .oldValue(deletedValue)
                .build();
    }
}
