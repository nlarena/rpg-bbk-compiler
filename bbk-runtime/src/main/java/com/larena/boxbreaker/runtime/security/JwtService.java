package com.larena.boxbreaker.runtime.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;

/** Issues and verifies HMAC-signed JWTs carrying the user and their special authorities. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${bbk.security.jwt.secret}") String secret,
                      @Value("${bbk.security.jwt.ttl-minutes}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlMinutes * 60_000L;
    }

    public String issue(String subject, Collection<String> authorities) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(subject)
            .claim("authorities", authorities)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(ttlMillis)))
            .signWith(key)
            .compact();
    }

    /** Parse and verify the token; throws {@link io.jsonwebtoken.JwtException} if invalid/expired. */
    public Jws<Claims> verify(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
