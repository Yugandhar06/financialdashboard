package com.finance.dashboard.dto.response;

import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * UserResponse — the public shape of a User object.
 *
 * CRITICAL: This DTO intentionally excludes the `password` field.
 * Never return password hashes in API responses, even though they are hashed.
 *
 * Used in: GET /api/users, GET /api/users/{id}
 */
@Getter
@Builder
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;

    /**
     * Factory method: converts User entity → UserResponse DTO.
     * Centralised here so any change to mapping happens in one place.
     */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}