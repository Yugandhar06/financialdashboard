package com.finance.dashboard.controller;

import com.finance.dashboard.dto.request.LoginRequest;
import com.finance.dashboard.dto.request.RegisterRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.AuthResponse;
import com.finance.dashboard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — public endpoints for registration and login.
 *
 * These routes are explicitly permitted in SecurityConfig (no JWT required).
 * All other routes require a valid Bearer token.
 *
 * Controller responsibilities (ONLY these, nothing more):
 *   1. Receive the HTTP request
 *   2. Validate the request body (@Valid)
 *   3. Delegate to the service
 *   4. Return an HTTP response
 *
 * Zero business logic here — that all lives in AuthService.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     *
     * Creates a new user account and returns a JWT for immediate use.
     * New users are assigned the VIEWER role by default (least privilege).
     *
     * Request body:
     * {
     *   "name": "Alice Smith",
     *   "email": "alice@example.com",
     *   "password": "securePass123"
     * }
     *
     * Response (201 Created):
     * {
     *   "success": true,
     *   "message": "Account created successfully",
     *   "data": {
     *     "token": "eyJhbGci...",
     *     "tokenType": "Bearer",
     *     "userId": 1,
     *     "name": "Alice Smith",
     *     "email": "alice@example.com",
     *     "role": "VIEWER"
     *   }
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse authResponse = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", authResponse));
    }

    /**
     * POST /api/auth/login
     *
     * Authenticates an existing user and returns a JWT.
     *
     * Request body:
     * {
     *   "email": "alice@example.com",
     *   "password": "securePass123"
     * }
     *
     * Response (200 OK):
     * {
     *   "success": true,
     *   "message": "Login successful",
     *   "data": { "token": "eyJhbGci...", "role": "VIEWER", ... }
     * }
     *
     * Error (401):
     * { "success": false, "message": "Invalid email or password" }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}