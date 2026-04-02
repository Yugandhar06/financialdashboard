package com.finance.dashboard.controller;

import com.finance.dashboard.dto.request.UpdateRoleRequest;
import com.finance.dashboard.dto.request.UpdateStatusRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.UserResponse;
import com.finance.dashboard.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserController — user management endpoints (Admin only).
 *
 * ACCESS CONTROL:
 * @PreAuthorize("hasRole('ADMIN')") on every method ensures only admins
 * can call these endpoints. If a VIEWER or ANALYST tries, Spring Security
 * throws AccessDeniedException → GlobalExceptionHandler returns 403.
 *
 * This is the SECOND layer of access control:
 *   Layer 1: JwtAuthFilter (are you authenticated?)
 *   Layer 2: @PreAuthorize (are you authorized for THIS action?)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")    // Class-level: all methods require ADMIN
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users
     * Returns a list of all users in the system.
     *
     * Response:
     * {
     *   "success": true,
     *   "data": [
     *     { "id": 1, "name": "Alice", "email": "...", "role": "VIEWER", "status": "ACTIVE" },
     *     { "id": 2, "name": "Bob",   "email": "...", "role": "ADMIN",  "status": "ACTIVE" }
     *   ]
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved", userService.getAllUsers()));
    }

    /**
     * GET /api/users/{id}
     * Returns a single user by their ID.
     *
     * Error (404): { "success": false, "message": "User not found with id: 99" }
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("User retrieved", userService.getUserById(id)));
    }

    /**
     * PATCH /api/users/{id}/role
     * Changes a user's role.
     *
     * Request body: { "role": "ANALYST" }
     *
     * Business rules enforced in UserService:
     *   - Admin cannot change their own role
     *   - Cannot demote the last admin
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("User role updated", userService.updateRole(id, request)));
    }

    /**
     * PATCH /api/users/{id}/status
     * Activates or deactivates a user account.
     *
     * Request body: { "status": "INACTIVE" }
     *
     * Business rule: Admin cannot deactivate themselves.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("User status updated", userService.updateStatus(id, request)));
    }

    /**
     * DELETE /api/users/{id}
     * Soft-deletes a user from the system.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }
}