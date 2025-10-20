package com.roovies.concertreservation.shared.util.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    private final SecretKey secretKey;

    public JwtUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 범용 토큰 생성 (만료시간을 인자로 받음)
     * @param subject
     * @param claims
     * @param expirationMillis
     * @return JWT 토큰 문자열
     */
    public String generateToken(String subject, Map<String, String> claims, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMillis);
        return Jwts.builder()
                .setSubject(subject)
                .setClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateAndGetClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getTokenType(String token) {
        return validateAndGetClaims(token).get("type", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            return validateAndGetClaims(token)
                    .getExpiration()
                    .toInstant()
                    .isBefore(Instant.now());
        } catch (Exception e) {
            return true;
        }
    }
}
