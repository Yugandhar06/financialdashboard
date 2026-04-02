package com.finance.dashboard.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * RegisterRequest — validated input for user registration.
 *
 * Validation annotations are enforced by @Valid in the controller.
 * If any constraint fails, Spring throws MethodArgumentNotValidException
 * which our GlobalExceptionHandler converts to a clean 400 error.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    // Minimum 8 chars — enforced here, not in DB (DB only stores the hash)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private com.finance.dashboard.enums.Role role;
}