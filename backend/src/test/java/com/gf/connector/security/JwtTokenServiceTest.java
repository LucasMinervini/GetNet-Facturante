package com.gf.connector.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService service;

    @BeforeEach
    void setup() {
        // Secret base64 de 32 bytes: "0123456789abcdef0123456789abcdef"
        String base64 = java.util.Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
        service = new JwtTokenService(base64, base64, 60, 120, "issuer", "aud");
    }

    @Test
    void generate_and_validate_access_token_roundtrip() {
        String token = service.generateAccessToken("user@example.com", Map.of("tenantId", "00000000-0000-0000-0000-000000000001"));
        String sub = service.validateAccessTokenAndGetSubject(token);
        assertThat(sub).isEqualTo("user@example.com");
        assertThat(service.getTenantIdClaim(token)).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(service.getTokenClaims(token)).isNotEmpty();
    }
}


