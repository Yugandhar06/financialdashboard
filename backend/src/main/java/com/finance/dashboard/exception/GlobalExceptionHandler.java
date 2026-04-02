package com.finance.dashboard.exception;

import com.finance.dashboard.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler — catches all exceptions and converts them
 * into consistent, client-friendly ApiResponse error objects.
 *
 * WHY CENTRALIZE ERROR HANDLING?
 * Without this, Spring would return different error shapes for different exceptions:
 *   - Validation errors → Spring's default BindingResult JSON
 *   - 404s → Spring's WhitelabelErrorPage HTML
 *   - Custom exceptions → Stack traces (exposing internals!)
 *
 * With this handler, EVERY error response looks like:
 * {
 *   "success": false,
 *   "message": "Human-readable error description",
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * HANDLER PRIORITY: More specific exceptions should be listed first.
 * Spring picks the most specific matching handler.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Validation Errors (400) ──────────────────────────────────────────

    /**
     * Handles @Valid failures on @RequestBody DTOs.
     * Collects ALL field errors into one response (not just the first one).
     *
     * Response example:
     * {
     *   "success": false,
     *   "message": "Validation failed: {amount: 'must be positive', category: 'required'}"
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed: " + fieldErrors));
    }

    // ── Business Logic Errors (400) ──────────────────────────────────────

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Resource Not Found (404) ─────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Duplicate Resource (409) ─────────────────────────────────────────

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // ── Authentication Failures (401) ────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        // NOTE: Return a vague message — don't reveal if email exists or just password is wrong
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledUser(DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Account is deactivated. Contact an administrator."));
    }

    // ── Authorization Failures (403) ─────────────────────────────────────

    /**
     * Thrown by Spring Security when @PreAuthorize check fails.
     * e.g., a VIEWER tries to call an ADMIN-only endpoint.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "Access denied. You do not have permission to perform this action."));
    }

    // ── Type Mismatch (400) ──────────────────────────────────────────────

    /**
     * Handles invalid enum values in query params.
     * e.g., GET /api/transactions?type=WRONG
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String message = String.format(
                "Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // ── Catch-All (500) ──────────────────────────────────────────────────

    /**
     * Safety net for any unhandled exception.
     * Logs the full stack trace for debugging but returns a generic message to the client.
     * NEVER expose internal error details to the client in production.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericError(Exception ex) {
        log.error("Unexpected error: ", ex); // full stack trace in logs
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again."));
    }
}