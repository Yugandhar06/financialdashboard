package com.finance.dashboard.service;

import com.finance.dashboard.dto.response.AuditLogResponse;
import com.finance.dashboard.entity.AuditLog;
import com.finance.dashboard.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AuditLogService — handles audit log recording and retrieval.
 *
 * Responsibilities:
 * 1. Record CREATE, UPDATE, DELETE operations
 * 2. Provide audit history for specific entities
 * 3. Generate compliance reports
 * 4. Answer "what changed" questions
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ── RECORDING OPERATIONS ─────────────────────────────────────────

    /**
     * Log a CREATE operation
     */
    public void logCreate(Long entityId, String entityType, String changedBy, String newValue) {
        AuditLog auditEntry = AuditLog.createLog(entityId, entityType, changedBy, newValue);
        auditLogRepository.save(auditEntry);
        log.info("Audit: {} {} created by {}", entityType, entityId, changedBy);
    }

    /**
     * Log an UPDATE operation
     */
    public void logUpdate(Long entityId, String entityType, String changedBy,
                         String fieldName, String oldValue, String newValue) {
        // Only log if value actually changed (prevent noise)
        if (oldValue != null && oldValue.equals(newValue)) {
            return;  // No change, no log
        }

        AuditLog auditEntry = AuditLog.updateLog(entityId, entityType, changedBy, fieldName, oldValue, newValue);
        auditLogRepository.save(auditEntry);
        log.info("Audit: {} {} field '{}' updated by {}: {} -> {}", 
                entityType, entityId, fieldName, changedBy, oldValue, newValue);
    }

    /**
     * Log a DELETE operation
     */
    public void logDelete(Long entityId, String entityType, String changedBy, String deletedValue) {
        AuditLog auditEntry = AuditLog.deleteLog(entityId, entityType, changedBy, deletedValue);
        auditLogRepository.save(auditEntry);
        log.info("Audit: {} {} deleted by {}", entityType, entityId, changedBy);
    }

    // ── RETRIEVAL OPERATIONS ─────────────────────────────────────────

    /**
     * Get audit history for a specific entity (paginated)
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditHistory(Long entityId, String entityType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByEntityIdAndEntityType(entityId, entityType, pageable)
                .map(AuditLogResponse::fromEntity);
    }

    /**
     * Get complete audit history for an entity (all records)
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getCompleteAuditHistory(Long entityId, String entityType) {
        return auditLogRepository.findByEntityIdAndEntityTypeOrderByTimestampDesc(entityId, entityType)
                .stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get recent changes (last N hours)
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecentChanges(int hoursBack) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hoursBack);
        LocalDateTime endTime = LocalDateTime.now();
        return auditLogRepository.findRecentChanges(startTime, endTime)
                .stream()
                .map(AuditLogResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get all changes made by a specific user (paginated)
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getChangesByUser(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByChangedByOrderByTimestampDesc(userEmail, pageable)
                .map(AuditLogResponse::fromEntity);
    }

    /**
     * Count total changes to an entity
     */
    @Transactional(readOnly = true)
    public long getChangeCount(Long entityId, String entityType) {
        return auditLogRepository.countByEntityIdAndEntityType(entityId, entityType);
    }

    /**
     * Get all DELETE operations (useful for seeing what was deleted)
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getDeletedRecords(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByActionOrderByTimestampDesc("DELETE", pageable)
                .map(AuditLogResponse::fromEntity);
    }
}
