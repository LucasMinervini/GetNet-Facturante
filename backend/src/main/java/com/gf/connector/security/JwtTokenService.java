package com.gf.connector.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtTokenService {

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;
    private final String audience;

    public JwtTokenService(
            @Value("${jwt.access.secret}") String accessSecretBase64,
            @Value("${jwt.refresh.secret}") String refreshSecretBase64,
            @Value("${jwt.access.ttl-seconds:900}") long accessTtlSeconds,
            @Value("${jwt.refresh.ttl-seconds:604800}") long refreshTtlSeconds,
            @Value("${jwt.issuer:gf-connector}") String issuer,
            @Value("${jwt.audience:getnet-facturante}") String audience
    ) {
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecretBase64));
        this.refreshKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecretBase64));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateAccessToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .addClaims(claims)
                .signWith(accessKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setAudience(audience)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .addClaims(claims)
                .signWith(refreshKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String validateAccessTokenAndGetSubject(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        if (claims.getIssuer() == null || !claims.getIssuer().equals(issuer)) {
            throw new IllegalArgumentException("Invalid token issuer");
        }
        if (claims.getAudience() == null || !claims.getAudience().equals(audience)) {
            throw new IllegalArgumentException("Invalid token audience");
        }
        return claims.getSubject();
    }

    public String getTenantIdClaim(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(accessKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("tenantId", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> getTokenClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(accessKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }
}


