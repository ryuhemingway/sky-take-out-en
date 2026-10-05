package com.sky.takeout.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import io.jsonwebtoken.Claims;

@Service
public class JwtService {
    private final SecretKey key;
    private final long ttlHours;
    public JwtService(@Value("${sky.jwt.secret}") String secret, @Value("${sky.jwt.ttl-hours:24}") long ttlHours) {
        if (secret.length() < 32) throw new IllegalArgumentException("sky.jwt.secret must be at least 32 characters");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.ttlHours = ttlHours;
    }
    public String create(Long employeeId, String username) {
        Instant now = Instant.now();
        return Jwts.builder().subject(username).claim("employeeId", employeeId).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttlHours * 3600))).signWith(key).compact();
    }
    public String createUser(Long userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder().subject(username).claim("userId", userId).claim("type", "user").issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttlHours * 3600))).signWith(key).compact();
    }
    public Claims claims(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
    public String username(String token) { return claims(token).getSubject(); }
}
