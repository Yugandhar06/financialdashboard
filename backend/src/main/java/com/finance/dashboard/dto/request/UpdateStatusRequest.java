package com.finance.dashboard.dto.request;

import com.finance.dashboard.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * UpdateStatusRequest — used by PATCH /api/users/{id}/status
 *
 * Admin-only operation. Sets user to ACTIVE or INACTIVE.
 * INACTIVE users cannot log in (blocked in CustomUserDetailsService).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "Status is required. Must be ACTIVE or INACTIVE")
    private UserStatus status;
}