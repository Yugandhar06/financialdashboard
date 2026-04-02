package com.finance.dashboard.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter — runs ONCE for every incoming HTTP request.
 *
 * Responsibility: Extract JWT from the Authorization header,
 * validate it, and set the authenticated user in Spring's SecurityContext.
 *
 * REQUEST FLOW THROUGH THIS FILTER:
 *
 *   Request arrives
 *       │
 *       ▼
 *   Is there an "Authorization: Bearer <token>" header?
 *       │ NO  → let request pass through (will fail at @PreAuthorize if needed)
 *       │ YES ↓
 *   Extract token, get email from it
 *       │
 *       ▼
 *   Is there already an authenticated user in the SecurityContext?
 *       │ YES → skip (already authenticated)
 *       │ NO  ↓
 *   Load user from DB, validate token
 *       │ INVALID → do nothing (request will be rejected by Spring Security)
 *       │ VALID   ↓
 *   Set user in SecurityContext → request is now "authenticated"
 *       │
 *       ▼
 *   Pass to next filter / controller
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // All JWTs must come as: "Authorization: Bearer <token>"
        // If header is missing or malformed, skip JWT processing
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip "Bearer " prefix (7 characters) to get the raw token
        final String token = authHeader.substring(7);
        final String email;

        try {
            email = jwtUtil.extractEmail(token);
        } catch (Exception e) {
            // Token is malformed — just continue without authentication
            // Spring Security will reject the request if the endpoint requires auth
            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate if not already authenticated (avoid double-processing)
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtUtil.validateToken(token, userDetails)) {
                // Create an authentication token with the user's roles
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities() // e.g., [ROLE_ADMIN]
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // Register this user as authenticated for this request
                // All downstream @PreAuthorize checks will use this
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}