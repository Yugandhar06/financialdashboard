package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.UpdateRoleRequest;
import com.finance.dashboard.dto.request.UpdateStatusRequest;
import com.finance.dashboard.dto.response.UserResponse;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.exception.BadRequestException;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService — business logic for user management.
 *
 * Key business rules enforced here:
 * 1. An admin cannot demote their own role (self-lockout prevention)
 * 2. An admin cannot deactivate themselves (self-lockout prevention)
 * 3. The last admin cannot be demoted (system must always have at least one admin)
 *
 * All DB access goes through UserRepository — no direct JPA calls here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Returns all users in the system (Admin only — enforced in controller).
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single user by ID.
     *
     * @throws ResourceNotFoundException if no user exists with that ID
     */
    public UserResponse getUserById(Long id) {
        User user = findUserOrThrow(id);
        return UserResponse.fromEntity(user);
    }

    /**
     * Changes a user's role.
     *
     * Business rules:
     * - Admin cannot change their own role (prevents accidental self-demotion)
     * - Cannot demote the last remaining admin (system integrity)
     */
    @Transactional
    public UserResponse updateRole(Long targetUserId, UpdateRoleRequest request) {
        String currentAdminEmail = getCurrentUserEmail();
        User targetUser = findUserOrThrow(targetUserId);

        // Rule 1: Admin cannot change their own role
        if (targetUser.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new BadRequestException(
                    "You cannot change your own role. Ask another admin to do this.");
        }

        // Rule 2: Cannot remove the last admin
        if (targetUser.getRole() == Role.ADMIN && request.getRole() != Role.ADMIN) {
            long adminCount = userRepository.findByRole(Role.ADMIN).size();
            if (adminCount <= 1) {
                throw new BadRequestException(
                        "Cannot demote the last admin. Promote another user first.");
            }
        }

        targetUser.setRole(request.getRole());
        User saved = userRepository.save(targetUser);
        log.info("User {} role changed to {} by {}", targetUserId, request.getRole(), currentAdminEmail);

        return UserResponse.fromEntity(saved);
    }

    /**
     * Activates or deactivates a user account.
     *
     * Business rule: Admin cannot deactivate themselves.
     */
    @Transactional
    public UserResponse updateStatus(Long targetUserId, UpdateStatusRequest request) {
        String currentAdminEmail = getCurrentUserEmail();
        User targetUser = findUserOrThrow(targetUserId);

        // Rule: Admin cannot deactivate their own account
        if (targetUser.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new BadRequestException(
                    "You cannot change your own account status.");
        }

        targetUser.setStatus(request.getStatus());
        User saved = userRepository.save(targetUser);
        log.info("User {} status changed to {} by {}", targetUserId, request.getStatus(), currentAdminEmail);

        return UserResponse.fromEntity(saved);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Hard-deletes a user from the system.
     * Business rule: Admin cannot delete themselves.
     */
    @Transactional
    public void deleteUser(Long targetUserId) {
        String currentAdminEmail = getCurrentUserEmail();
        User targetUser = findUserOrThrow(targetUserId);

        if (targetUser.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw new BadRequestException("You cannot delete your own account.");
        }

        userRepository.delete(targetUser);
        log.info("User {} deleted by {}", targetUserId, currentAdminEmail);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + id));
    }

    /**
     * Gets the email of the currently authenticated admin from the SecurityContext.
     * The SecurityContext is populated by JwtAuthFilter on every request.
     */
    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();  // getName() returns the email (our JWT subject)
    }
}