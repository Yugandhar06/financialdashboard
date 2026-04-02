package com.finance.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ApiResponse<T> — a unified wrapper for ALL API responses.
 *
 * WHY a wrapper? Because consistent response shapes make the frontend
 * predictable. Instead of returning raw objects from some endpoints
 * and error strings from others, every response looks like:
 *
 * SUCCESS:
 * {
 *   "success": true,
 *   "message": "Transaction created",
 *   "data": { ...actual payload... },
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * ERROR:
 * {
 *   "success": false,
 *   "message": "Amount must be positive",
 *   "data": null,
 *   "timestamp": "2024-01-15T10:30:00"
 * }
 *
 * The generic <T> means data can be any type:
 *   ApiResponse<TransactionResponse>        → single record
 *   ApiResponse<Page<TransactionResponse>>  → paginated list
 *   ApiResponse<DashboardSummaryResponse>   → dashboard data
 *   ApiResponse<Void>                       → no payload (e.g., delete)
 *
 * @JsonInclude(NON_NULL) → null fields are omitted from JSON output (cleaner)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    // ── Static factory methods for clean call sites ──────────────────

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}