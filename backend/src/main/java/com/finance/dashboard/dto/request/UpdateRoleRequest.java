package com.finance.dashboard.dto.request;

import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * UpdateRoleRequest — used by PATCH /api/users/{id}/role
 *
 * Admin-only operation. Allows changing a user's access level.
 * Assumption: An admin cannot demote themselves
 * (enforced in UserService to prevent accidental lockout).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {

    @NotNull(message = "Role is required. Must be VIEWER, ANALYST, or ADMIN")
    private Role role;
}