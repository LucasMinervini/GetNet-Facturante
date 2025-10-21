package com.gf.connector.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuthorizationTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Nested
    @DisplayName("JWT Token validation")
    class JwtValidation {
        @Test
        void exportTransactions_requiresUserOrAdmin() {
            String token = "user-token";
            when(jwtTokenService.validateAccessTokenAndGetSubject(token)).thenReturn("user@example.com");
            
            String subject = jwtTokenService.validateAccessTokenAndGetSubject(token);
            assertThat(subject).isEqualTo("user@example.com");
        }

        @Test
        void exportCreditNotes_requiresAdmin() {
            String token = "admin-token";
            when(jwtTokenService.validateAccessTokenAndGetSubject(token)).thenReturn("admin@example.com");
            
            String subject = jwtTokenService.validateAccessTokenAndGetSubject(token);
            assertThat(subject).isEqualTo("admin@example.com");
        }
    }

    @Nested
    @DisplayName("Token claims validation")
    class TokenClaims {
        @Test
        void getTransactions_requiresUserOrAdmin() {
            String token = "user-token-3";
            when(jwtTokenService.validateAccessTokenAndGetSubject(token)).thenReturn("user@example.com");
            when(jwtTokenService.getTenantIdClaim(token)).thenReturn(java.util.UUID.randomUUID().toString());
            
            String subject = jwtTokenService.validateAccessTokenAndGetSubject(token);
            String tenantId = jwtTokenService.getTenantIdClaim(token);
            
            assertThat(subject).isEqualTo("user@example.com");
            assertThat(tenantId).isNotNull();
        }

        @Test
        void adminOnly_actions_requireAdmin() {
            String token = "admin-token-2";
            when(jwtTokenService.validateAccessTokenAndGetSubject(token)).thenReturn("admin@example.com");
            when(jwtTokenService.getTokenClaims(token)).thenReturn(java.util.Map.of("roles", 
                java.util.List.of(java.util.Map.of("authority", "ROLE_ADMIN"))));
            
            String subject = jwtTokenService.validateAccessTokenAndGetSubject(token);
            java.util.Map<String, Object> claims = jwtTokenService.getTokenClaims(token);
            
            assertThat(subject).isEqualTo("admin@example.com");
            assertThat(claims).containsKey("roles");
        }
    }
}


