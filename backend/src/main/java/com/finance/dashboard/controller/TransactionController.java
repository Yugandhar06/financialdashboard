package com.finance.dashboard.controller;

import com.finance.dashboard.dto.request.CreateTransactionRequest;
import com.finance.dashboard.dto.request.UpdateTransactionRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * TransactionController — financial record CRUD operations.
 *
 * ROLE-BASED ACCESS CONTROL (RBAC):
 *
 * VIEWER   → No access to transaction endpoints (dashboard only)
 * ANALYST  → GET only (read transactions and records)
 * ADMIN    → Full access (GET, POST, PUT, DELETE)
 *
 * Access breakdown:
 * GET  /api/admin/transactions     → ANALYST, ADMIN
 * GET  /api/admin/transactions/{id} → ANALYST, ADMIN
 * POST /api/admin/transactions     → ADMIN only
 * PUT  /api/admin/transactions/{id} → ADMIN only
 * DELETE /api/admin/transactions/{id} → ADMIN only
 *
 * Filtering is done via query parameters (all optional):
 *   GET /api/transactions?type=EXPENSE&category=Food&startDate=2024-01-01&page=0&size=10
 */
@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // ── CREATE (Admin only) ──────────────────────────────────────────────

    /**
     * POST /api/transactions
     * Creates a new financial record.
     *
     * Request body:
     * {
     *   "amount": 5000.00,
     *   "type": "INCOME",
     *   "category": "Salary",
     *   "date": "2024-01-15",
     *   "notes": "Monthly salary for January"
     * }
     *
     * Response (201 Created):
     * { "success": true, "message": "Transaction created", "data": { ...record } }
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {

        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

    // ── READ (All authenticated roles) ───────────────────────────────────

    /**
     * GET /api/transactions
     * Returns a paginated, optionally filtered list of transactions.
     *
     * Query parameters (all optional):
     *   type      → INCOME or EXPENSE
     *   category  → partial match, case-insensitive (e.g., "food" matches "Food")
     *   startDate → inclusive, format: yyyy-MM-dd
     *   endDate   → inclusive, format: yyyy-MM-dd
     *   page      → zero-indexed page number (default: 0)
     *   size      → records per page (default: 10)
     *
     * Response includes pagination metadata (totalElements, totalPages, etc.)
     * from Spring's Page<T> wrapper.
     *
     * Examples:
     *   GET /api/transactions                              → all records, page 1
     *   GET /api/transactions?type=EXPENSE                 → expenses only
     *   GET /api/transactions?startDate=2024-01-01&endDate=2024-03-31 → Q1 records
     *   GET /api/transactions?category=food&page=1&size=5  → food, page 2
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        // Cap page size to prevent abuse (client can't request 10,000 records at once)
        int safeSize = Math.min(size, 100);

        Page<TransactionResponse> result = transactionService
                .getTransactions(type, category, startDate, endDate, page, safeSize);

        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved", result));
    }

    /**
     * GET /api/transactions/{id}
     * Returns a single transaction by ID.
     *
     * Error (404): { "success": false, "message": "Transaction not found with id: 99" }
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ApiResponse.success("Transaction retrieved",
                        transactionService.getTransactionById(id)));
    }

    // ── UPDATE (Admin only) ──────────────────────────────────────────────

    /**
     * PUT /api/transactions/{id}
     * Partially updates a transaction (only provided fields are changed).
     *
     * Request body (all fields optional — only send what you want to change):
     * { "amount": 6000.00 }                     → updates only amount
     * { "category": "Freelance", "notes": "..." } → updates category and notes
     *
     * Response (200 OK):
     * { "success": true, "data": { ...updated record } }
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Transaction updated",
                        transactionService.updateTransaction(id, request)));
    }

    // ── DELETE (Admin only, soft delete) ─────────────────────────────────

    /**
     * DELETE /api/transactions/{id}
     * Soft-deletes a transaction (sets is_deleted = true).
     *
     * The record is NOT removed from the database.
     * It becomes invisible to all queries but remains for audit purposes.
     *
     * Response (200 OK):
     * { "success": true, "message": "Transaction deleted successfully" }
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully"));
    }
}