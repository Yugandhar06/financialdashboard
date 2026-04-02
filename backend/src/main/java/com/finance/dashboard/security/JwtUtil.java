package com.finance.dashboard.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * JwtUtil — handles all JWT operations:
 *   1. generateToken()  → creates a signed JWT after successful login
 *   2. validateToken()  → verifies signature + expiry on every request
 *   3. extractEmail()   → pulls the user's email from the token payload
 *
 * TOKEN STRUCTURE (what's inside the JWT):
 *   Header  : { alg: "HS256", typ: "JWT" }
 *   Payload : { sub: "user@email.com", iat: <issued-at>, exp: <expires-at> }
 *   Signature: HMAC_SHA256(header + payload, secret)
 *
 * WHY JWT? → Stateless: no session storage needed on the server.
 * The token itself carries identity. Just validate the signature on each request.
 */
@Component
public class JwtUtil {

    // Secret key from application.properties — must be 256+ bits for HS256
    @Value("${jwt.secret}")
    private String secretKey;

    // Token expiry in milliseconds (default: 86400000 = 24 hours)
    @Value("${jwt.expiration.ms}")
    private long expirationMs;

    /**
     * Builds the signing key from the configured secret.
     * Uses HMAC-SHA256 (symmetric — same key signs and verifies).
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Generates a JWT token for a successfully authenticated user.
     * The email is stored as the "subject" — this is how we identify the user
     * on subsequent requests without hitting the database.
     *
     * @param userDetails Spring Security user object (email + authorities)
     * @return Signed JWT string (e.g., "eyJhbGciOiJIUzI1NiJ9...")
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())     // email
                .setIssuedAt(new Date())                   // current time
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the email (subject) from the token.
     * Called by JwtAuthFilter to know which user made the request.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates the token against the current user's details.
     * Checks: email matches + token is not expired.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String email = extractEmail(token);
            return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Token is malformed, tampered, or expired
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}