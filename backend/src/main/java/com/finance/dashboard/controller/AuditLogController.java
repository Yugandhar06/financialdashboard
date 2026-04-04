package com.finance.dashboard.controller;

import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.AuditLogResponse;
import com.finance.dashboard.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AuditLogController — audit trail and compliance endpoints.
 *
 * Access: Admin only — only admins can view full audit logs.
 * Endpoint base: /api/admin/audit
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * GET /api/admin/audit/transactions/{id}
     * Returns the complete change history for a specific transaction.
     *
     * Shows: who changed it, when, what field, old vs new value.
     * Use case: "Show me what happened to this transaction"
     *
     * Example response:
     * [
     *   {action: "CREATE", newValue: "5000", timestamp: "2026-04-04T10:00:00", changedBy: "admin@..."},
     *   {action: "UPDATE", fieldName: "amount", oldValue: "5000", newValue: "6000", timestamp: "2026-04-04T11:00:00", changedBy: "admin@..."},
     *   {action: "UPDATE", fieldName: "category", oldValue: "Salary", newValue: "Freelance", timestamp: "2026-04-04T12:00:00", changedBy: "admin@..."}
     * ]
     */
    @GetMapping("/transactions/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getTransactionAuditHistory(
            @PathVariable Long transactionId) {

        List<AuditLogResponse> history = auditLogService.getCompleteAuditHistory(transactionId, "TRANSACTION");
        return ResponseEntity.ok(
                ApiResponse.success("Transaction audit history retrieved", history));
    }

    /**
     * GET /api/admin/audit/users/{id}
     * Returns the complete change history for a specific user.
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getUserAuditHistory(
            @PathVariable Long userId) {

        List<AuditLogResponse> history = auditLogService.getCompleteAuditHistory(userId, "USER");
        return ResponseEntity.ok(
                ApiResponse.success("User audit history retrieved", history));
    }

    /**
     * GET /api/admin/audit/history?page=0&size=10
     * Paginated audit history for a specific entity.
     *
     * Query params: entityId, entityType (TRANSACTION or USER), page, size
     * Example: /api/admin/audit/history?entityId=5&entityType=TRANSACTION&page=0&size=10
     */
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditHistory(
            @RequestParam Long entityId,
            @RequestParam String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditLogResponse> history = auditLogService.getAuditHistory(entityId, entityType, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Audit history retrieved", history));
    }

    /**
     * GET /api/admin/audit/recent?hours=24
     * Returns recent changes across all entities in the last N hours.
     *
     * Use case: "What changed in the last 24 hours?"
     * Query params: hours (default: 24)
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecentChanges(
            @RequestParam(defaultValue = "24") int hours) {

        List<AuditLogResponse> recent = auditLogService.getRecentChanges(hours);
        return ResponseEntity.ok(
                ApiResponse.success("Recent changes retrieved (last " + hours + " hours)", recent));
    }

    /**
     * GET /api/admin/audit/user?email=admin@example.com&page=0&size=10
     * Returns all changes made by a specific user.
     *
     * Use case: Audit trail of a specific admin's actions.
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getChangesByUser(
            @RequestParam String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditLogResponse> changes = auditLogService.getChangesByUser(email, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Changes by user retrieved", changes));
    }

    /**
     * GET /api/admin/audit/deleted?page=0&size=10
     * Returns all deleted records (soft deletes).
     *
     * Use case: "Show me what was deleted when"
     * Helps with recovery and compliance.
     */
    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getDeletedRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditLogResponse> deleted = auditLogService.getDeletedRecords(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Deleted records retrieved", deleted));
    }

    /**
     * GET /api/admin/audit/count?entityId=5&entityType=TRANSACTION
     * Returns the total number of changes to an entity.
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getChangeCount(
            @RequestParam Long entityId,
            @RequestParam String entityType) {

        long count = auditLogService.getChangeCount(entityId, entityType);
        return ResponseEntity.ok(
                ApiResponse.success("Change count retrieved", count));
    }
}
