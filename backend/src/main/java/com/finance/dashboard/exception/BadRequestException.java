package com.finance.dashboard.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the request is syntactically valid but logically incorrect.
 * Maps to HTTP 400 Bad Request.
 *
 * Examples:
 *   - Admin trying to demote themselves
 *   - Trying to demote the last admin
 *   - Date range where start > end
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}