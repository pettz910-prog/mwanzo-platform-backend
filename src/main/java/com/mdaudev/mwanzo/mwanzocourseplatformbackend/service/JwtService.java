package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT Service
 *
 * Handles JWT token generation, validation, and parsing.
 * Uses HMAC-SHA256 for token signing.
 *
 * Token Structure:
 * - Subject: User ID (UUID)
 * - Claims: email, role, name
 * - Expiration: 24 hours (configurable)
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Service
@Slf4j
public class JwtService {

    /**
     * Secret key for JWT signing.
     * Should be at least 256 bits (32 bytes) for HS256.
     * In production, load from environment variable or secure vault.
     */
    @Value("${jwt.secret:mwanzo-secret-key-change-this-in-production-minimum-256-bits}")
    private String SECRET_KEY;

    /**
     * JWT token expiration time in milliseconds.
     * Default: 24 hours (86400000 ms)
     */
    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    /**
     * Generate JWT token for user.
     *
     * @param user User entity
     * @return JWT token string
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("name", user.getName());

        return createToken(claims, user.getId().toString());
    }

    /**
     * Create JWT token with claims.
     *
     * @param claims Additional claims to include
     * @param subject Token subject (user ID)
     * @return JWT token string
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Get signing key from secret.
     *
     * @return Signing key
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * Extract user ID from token.
     *
     * @param token JWT token
     * @return User ID as UUID
     */
    public UUID extractUserId(String token) {
        String userIdString = extractClaim(token, Claims::getSubject);
        return UUID.fromString(userIdString);
    }

    /**
     * Extract email from token.
     *
     * @param token JWT token
     * @return User's email
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Extract role from token.
     *
     * @param token JWT token
     * @return User's role
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extract expiration date from token.
     *
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract specific claim from token.
     *
     * @param token JWT token
     * @param claimsResolver Function to extract claim
     * @param <T> Claim type
     * @return Extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token.
     *
     * @param token JWT token
     * @return All claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if token is expired.
     *
     * @param token JWT token
     * @return true if expired
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token against user details.
     *
     * @param token JWT token
     * @param userDetails Spring Security user details
     * @return true if valid
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token (without user details check).
     *
     * @param token JWT token
     * @return true if valid and not expired
     */
    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}