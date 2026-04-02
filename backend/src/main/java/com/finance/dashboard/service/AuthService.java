package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.LoginRequest;
import com.finance.dashboard.dto.request.RegisterRequest;
import com.finance.dashboard.dto.response.AuthResponse;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.exception.ConflictException;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.security.CustomUserDetailsService;
import com.finance.dashboard.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService — handles registration and login business logic.
 *
 * REGISTRATION FLOW:
 *   1. Check email is not already taken
 *   2. Hash the plain-text password with BCrypt
 *   3. Save new user (default role: VIEWER — principle of least privilege)
 *   4. Generate JWT and return it
 *
 * LOGIN FLOW:
 *   1. Delegate to Spring's AuthenticationManager
 *      → It checks: does user exist? Is password correct? Is account active?
 *   2. If authentication succeeds, generate and return a JWT
 *   3. If it fails, AuthenticationManager throws an exception automatically
 *      (handled by GlobalExceptionHandler → 401 response)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Registers a new user and returns a JWT so they're immediately logged in.
     *
     * @param request name, email, password (plain text — hashed here)
     * @return AuthResponse with JWT token and user info
     * @throws ConflictException if email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Guard: prevent duplicate email registrations
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("An account with this email already exists");
        }

        // Build the user entity — note: password is hashed, role defaults to VIEWER
        com.finance.dashboard.enums.Role newRole = com.finance.dashboard.enums.Role.VIEWER;
        if (request.getRole() != null && request.getRole() != com.finance.dashboard.enums.Role.ADMIN) {
            newRole = request.getRole();
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())   // normalize email
                .password(passwordEncoder.encode(request.getPassword())) // hash it
                .role(newRole)
                .build();
        // role = VIEWER and status = ACTIVE by @Builder.Default in the entity

        User savedUser = userRepository.save(user);
        log.info("New user registered: {} (id={})", savedUser.getEmail(), savedUser.getId());

        // Generate token so user is immediately authenticated after registration
        String token = generateTokenForEmail(savedUser.getEmail());

        return buildAuthResponse(token, savedUser);
    }

    /**
     * Authenticates a user and returns a JWT.
     *
     * We delegate the actual authentication (password check) to Spring Security's
     * AuthenticationManager. This is the correct approach — don't roll your own
     * credential verification logic.
     *
     * @param request email + plain-text password
     * @return AuthResponse with JWT token and user info
     */
    public AuthResponse login(LoginRequest request) {
        // This single call handles:
        //   - Looking up the user by email
        //   - Comparing the provided password against the BCrypt hash
        //   - Checking if the account is ACTIVE
        // Throws BadCredentialsException or DisabledException on failure
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        // Authentication passed — fetch full user details for the response
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(); // Can't happen — authenticate() would have failed first

        String token = generateTokenForEmail(user.getEmail());
        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(token, user);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private String generateTokenForEmail(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtUtil.generateToken(userDetails);
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}