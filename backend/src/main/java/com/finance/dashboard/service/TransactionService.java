package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.CreateTransactionRequest;
import com.finance.dashboard.dto.request.UpdateTransactionRequest;
import com.finance.dashboard.dto.response.TransactionResponse;
import com.finance.dashboard.entity.Transaction;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.TransactionType;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.repository.TransactionRepository;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * TransactionService — business logic for financial record management.
 *
 * Responsibilities:
 * 1. CREATE  — validates, associates with current user, persists
 * 2. READ    — paginated + filtered listing, single record lookup
 * 3. UPDATE  — partial update (only provided fields are changed)
 * 4. DELETE  — soft delete (sets is_deleted = true, never removes the row)
 *
 * Key design decisions:
 * - All queries use Specifications to respect the soft-delete filter
 * - User association on create tracks who entered each record (audit trail)
 * - Partial update pattern: null fields in request = "don't change this"
 * - @Transactional on write operations ensures DB consistency
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    // ── CREATE ───────────────────────────────────────────────────────────

    /**
     * Creates a new transaction and associates it with the currently logged-in user.
     *
     * The user association serves two purposes:
     * 1. Audit trail: we know who entered this record
     * 2. Future: filter by user if needed
     */
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        User currentUser = getCurrentUser();

        Transaction transaction = Transaction.builder()
                .user(currentUser)
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .date(request.getDate())
                .notes(request.getNotes())
                // isDeleted defaults to false (set by @Builder.Default in entity)
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: id={}, type={}, amount={} by user={}",
                saved.getId(), saved.getType(), saved.getAmount(), currentUser.getEmail());

        return TransactionResponse.fromEntity(saved);
    }

    // ── READ (paginated + filtered) ──────────────────────────────────────

    /**
     * Returns a paginated, filtered list of transactions.
     *
     * All parameters are optional:
     * - If type is null      → no type filter
     * - If category is null  → no category filter
     * - If dates are null    → no date range filter
     * - If none provided     → returns all non-deleted transactions
     *
     * Pagination defaults: page=0, size=10, sorted by date DESC.
     *
     * @param type      INCOME or EXPENSE (nullable)
     * @param category  category search string (nullable, partial match)
     * @param startDate inclusive start of date range (nullable)
     * @param endDate   inclusive end of date range (nullable)
     * @param page      zero-indexed page number
     * @param size      number of records per page
     */
    public Page<TransactionResponse> getTransactions(
            TransactionType type,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {

        // Build dynamic WHERE clause from whichever filters are non-null
        Specification<Transaction> spec = TransactionSpecification.withFilters(
                type, category, startDate, endDate);

        // Sort by business date descending (most recent first), then by creation time
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "date", "createdAt"));

        return transactionRepository.findAll(spec, pageable)
                .map(TransactionResponse::fromEntity);
    }

    // ── READ (single) ────────────────────────────────────────────────────

    /**
     * Returns a single transaction by ID.
     * Respects soft delete: throws 404 if the record is soft-deleted.
     */
    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = findActiveTransactionOrThrow(id);
        return TransactionResponse.fromEntity(transaction);
    }

    // ── UPDATE ───────────────────────────────────────────────────────────

    /**
     * Partially updates a transaction.
     *
     * PARTIAL UPDATE PATTERN:
     * We only update a field if its value in the request is non-null.
     * This allows the client to send only the fields they want to change.
     *
     * Example: { "amount": 500.00 } → only amount is updated, rest stays the same.
     */
    @Transactional
    public TransactionResponse updateTransaction(Long id, UpdateTransactionRequest request) {
        Transaction transaction = findActiveTransactionOrThrow(id);

        // Apply only the changes that were actually provided
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getType() != null) {
            transaction.setType(request.getType());
        }
        if (request.getCategory() != null) {
            transaction.setCategory(request.getCategory().trim());
        }
        if (request.getDate() != null) {
            transaction.setDate(request.getDate());
        }
        if (request.getNotes() != null) {
            transaction.setNotes(request.getNotes());
        }

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction updated: id={}", id);

        return TransactionResponse.fromEntity(saved);
    }

    // ── DELETE (soft) ────────────────────────────────────────────────────

    /**
     * Soft-deletes a transaction by setting is_deleted = true.
     *
     * WHY SOFT DELETE?
     * Financial records must never be physically removed:
     * - Legal/audit compliance requirements
     * - Ability to recover accidental deletions
     * - Dashboard history remains accurate for past periods
     *
     * After deletion, the record is invisible to all queries but still in the DB.
     */
    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = findActiveTransactionOrThrow(id);
        transaction.setIsDeleted(true);
        transactionRepository.save(transaction);
        log.info("Transaction soft-deleted: id={}", id);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Finds a non-deleted transaction or throws 404.
     * Used by get, update, and delete operations.
     */
    private Transaction findActiveTransactionOrThrow(Long id) {
        return transactionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found with id: " + id));
    }

    /**
     * Gets the currently authenticated user entity from the database.
     * Email comes from the JWT (set in SecurityContext by JwtAuthFilter).
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Authenticated user not found in database"));
    }
}