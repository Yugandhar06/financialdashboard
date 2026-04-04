package com.finance.dashboard.config;

import com.finance.dashboard.security.JwtAuthFilter;
import com.finance.dashboard.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * SecurityConfig — the central security setup for the application.
 *
 * Two layers of access control work together here:
 *
 * LAYER 1 — URL-level rules (in filterChain):
 *   - /api/auth/** is PUBLIC (login, register — no token needed)
 *   - All other endpoints require a valid JWT
 *
 * LAYER 2 — Method-level rules (@PreAuthorize in controllers):
 *   - @PreAuthorize("hasRole('ADMIN')") → only admins can call this method
 *   - @PreAuthorize("hasAnyRole('ANALYST','ADMIN')") → analyst or admin
 *   - Enabled by: @EnableMethodSecurity
 *
 * WHY TWO LAYERS?
 * URL rules handle authentication (are you logged in?).
 * Method rules handle authorization (are you allowed to do THIS?).
 * Separating them keeps the logic clean and explicit.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // Activates @PreAuthorize annotations in controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            // Disable CSRF — not needed for stateless JWT APIs
            .csrf(csrf -> csrf.disable())

            // Define which endpoints are public vs protected
            .authorizeHttpRequests(auth -> auth
                    // Auth endpoints are public — anyone can register/login
                    .requestMatchers("/api/auth/**").permitAll()
                    // URL Level RBAC for Role-Based Dashboards
                    .requestMatchers("/api/viewer/**").hasAnyRole("VIEWER", "ANALYST", "ADMIN")
                    .requestMatchers("/api/analyst/**").hasAnyRole("ANALYST", "ADMIN")
                    // Transactions: authenticated users can access (method-level @PreAuthorize handles role validation)
                    .requestMatchers("/api/admin/transactions", "/api/admin/transactions/*").authenticated()
                    // Users endpoint: ADMIN only (all operations)
                    .requestMatchers("/api/admin/users/**").hasRole("ADMIN")
                    // All other /api/admin/** endpoints require ADMIN
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // Everything else requires a valid JWT token
                    .anyRequest().authenticated()
            )

            // STATELESS session — Spring won't create/use HTTP sessions
            // Every request must carry its own JWT token
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Wire our custom auth provider (uses our UserDetailsService + BCrypt)
            .authenticationProvider(authenticationProvider())

            // Run our JWT filter BEFORE Spring's default auth filter
            // This is how JWT tokens get recognized before Spring tries basic auth
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Wires together: our UserDetailsService + BCrypt password encoder.
     * Spring Security uses this to verify passwords during login.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt password encoder with default cost factor (10).
     * Used when creating users (hashing) and when logging in (verifying).
     * BCrypt is intentionally slow — this is a security feature against brute force.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Exposes Spring's AuthenticationManager as a bean.
     * Used by AuthService to programmatically authenticate during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Resolves CORS automatically prior to Spring Security checking Authorization.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow Vite Frontend URL
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://127.0.0.1:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}