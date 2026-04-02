package com.finance.dashboard.entity;

import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User Entity — maps to the `users` table in MySQL.
 *
 * Design notes:
 * - Password is stored as bcrypt hash, NEVER plain text
 * - Role is stored as a string enum (e.g., "ADMIN") so the DB is self-describing
 * - Status allows admins to deactivate accounts without deletion
 * - transactions is a lazy-loaded collection (not fetched unless explicitly needed)
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // Unique constraint enforced at both DB and application level
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    // Always store bcrypt-hashed password (cost factor 10+)
    @Column(nullable = false)
    private String password;

    // EnumType.STRING → stores "ADMIN" not "2" — readable without a lookup table
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.VIEWER; // New users default to least privilege

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    // Audit timestamps — set automatically by Hibernate
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Lazy: transactions are NOT loaded when you fetch a user
    // (avoids accidentally loading thousands of rows)
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions;
}