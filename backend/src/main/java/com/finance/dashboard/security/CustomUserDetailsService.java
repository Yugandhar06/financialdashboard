package com.finance.dashboard.security;

import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tells Spring Security how to load a user from our database.
 *
 * Spring Security calls loadUserByUsername() during:
 *   1. Login — to verify the password
 *   2. JWT validation — to get the user's current role/status
 *
 * We wrap the User entity into Spring's UserDetails interface,
 * which exposes: username, password, and authorities (roles).
 *
 * IMPORTANT: We prefix the role with "ROLE_" because Spring Security
 * expects this convention when using hasRole() checks.
 * e.g., Role.ADMIN → "ROLE_ADMIN"
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Look up by email (our system uses email as the login identifier)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with email: " + email));

        // Block inactive users from authenticating
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UsernameNotFoundException("User account is deactivated: " + email);
        }

        // Convert our Role enum to a Spring Security GrantedAuthority
        // "ROLE_ADMIN", "ROLE_ANALYST", "ROLE_VIEWER"
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(authority)
        );
    }
}