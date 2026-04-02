package com.finance.dashboard.repository;

import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository — data access layer for the `users` table.
 *
 * Extends JpaRepository which auto-provides:
 *   save(), findById(), findAll(), delete(), count(), existsById()
 *
 * We only need to declare CUSTOM queries here.
 * Spring Data JPA generates the SQL from the method name at startup.
 *
 * e.g., findByEmail(String email)
 *   → SELECT * FROM users WHERE email = ?
 *
 * NO business logic here. This layer only speaks SQL/JPA.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email — used during login and JWT validation.
     * Returns Optional to force the caller to handle "not found" explicitly.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered — used during registration
     * to give a helpful "email already in use" error instead of a DB constraint error.
     */
    boolean existsByEmail(String email);

    /**
     * Find all users with a specific role — useful for admin user-management views.
     */
    List<User> findByRole(Role role);
}