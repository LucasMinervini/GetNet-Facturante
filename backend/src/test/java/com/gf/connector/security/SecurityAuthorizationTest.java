package com.gf.connector.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void mockJwt(String token, String username, String tenantId, String... roles) {
        when(jwtTokenService.validateAccessTokenAndGetSubject(token)).thenReturn(username);
        when(jwtTokenService.getTenantIdClaim(token)).thenReturn(tenantId);
        // Simula claims con authorities como espera JwtAuthenticationFilter
        java.util.List<java.util.Map<String, String>> roleMaps = new java.util.ArrayList<>();
        for (String role : roles) {
            roleMaps.add(java.util.Map.of("authority", role));
        }
        when(jwtTokenService.getTokenClaims(token)).thenReturn(java.util.Map.of("roles", roleMaps));
    }

    @Nested
    @DisplayName("Reports endpoints authorization")
    class Reports {
        @Test
        void exportTransactions_requiresUserOrAdmin() throws Exception {
            // Sin token => 401
            mockMvc.perform(get("/api/reports/transactions/export"))
                .andExpect(status().isUnauthorized());

            // Con USER => 200 (controlador requiere datos; aquí solo validamos seguridad)
            String userToken = "user-token";
            mockJwt(userToken, "user@example.com", java.util.UUID.randomUUID().toString(), "ROLE_USER");
            mockMvc.perform(get("/api/reports/transactions/export").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk());
        }

        @Test
        void exportCreditNotes_requiresAdmin() throws Exception {
            // USER => 403
            String userToken = "user-token-2";
            mockJwt(userToken, "user@example.com", java.util.UUID.randomUUID().toString(), "ROLE_USER");
            mockMvc.perform(get("/api/reports/credit-notes/export").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

            // ADMIN => 200
            String adminToken = "admin-token";
            mockJwt(adminToken, "admin@example.com", java.util.UUID.randomUUID().toString(), "ROLE_ADMIN");
            mockMvc.perform(get("/api/reports/credit-notes/export").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Transactions endpoints authorization")
    class Transactions {
        @Test
        void getTransactions_requiresUserOrAdmin() throws Exception {
            mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());

            String userToken = "user-token-3";
            mockJwt(userToken, "user@example.com", java.util.UUID.randomUUID().toString(), "ROLE_USER");
            mockMvc.perform(get("/api/transactions").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk());
        }

        @Test
        void adminOnly_actions_requireAdmin() throws Exception {
            String userToken = "user-token-4";
            mockJwt(userToken, "user@example.com", java.util.UUID.randomUUID().toString(), "ROLE_USER");
            mockMvc.perform(post("/api/transactions/initialize-billing-status").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isForbidden());

            String adminToken = "admin-token-2";
            mockJwt(adminToken, "admin@example.com", java.util.UUID.randomUUID().toString(), "ROLE_ADMIN");
            mockMvc.perform(post("/api/transactions/initialize-billing-status").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        }
    }
}


