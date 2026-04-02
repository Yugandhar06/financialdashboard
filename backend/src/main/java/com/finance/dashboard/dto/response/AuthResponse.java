package com.finance.dashboard.dto.response;

import com.finance.dashboard.enums.Role;
import lombok.*;

/**
 * Returned after a successful login.
 * Contains the JWT token and basic user info for the frontend.
 *
 * NOTE: We return the role so the frontend can show/hide UI elements
 * immediately (e.g., hide "Create Transaction" button for Viewers).
 * However, the backend ALWAYS re-validates the role on every API call —
 * hiding UI elements is cosmetic only, not a security measure.
 */
@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;    // Always "Bearer"
    private Long userId;
    private String name;
    private String email;
    private Role role;
}