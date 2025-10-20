package com.gf.connector.performance;

import com.gf.connector.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PerformanceSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    private String token;
    private String tenantId;

    @BeforeEach
    void setup() {
        token = "perf-user-token";
        tenantId = java.util.UUID.randomUUID().toString();
        java.util.List<java.util.Map<String, String>> roleMaps = java.util.List.of(java.util.Map.of("authority", "ROLE_USER"));
        when(jwtTokenService.validateAccessTokenAndGetSubject(token)).thenReturn("user@example.com");
        when(jwtTokenService.getTenantIdClaim(token)).thenReturn(tenantId);
        when(jwtTokenService.getTokenClaims(token)).thenReturn(java.util.Map.of("roles", roleMaps));
    }

    private String bearer() { return "Bearer " + token; }

    @Test
    void transactions_list_should_be_fast_under_small_load() throws Exception {
        int requests = 50; // smoke bajo
        long start = System.currentTimeMillis();
        for (int i = 0; i < requests; i++) {
            mockMvc.perform(get("/api/transactions")
                    .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());
        }
        long elapsedMs = System.currentTimeMillis() - start;
        // Umbral conservador para entorno local/CI: 5s para 50 requests
        org.assertj.core.api.Assertions.assertThat(elapsedMs)
            .withFailMessage("Demasiado lento: %sms para %s requests", elapsedMs, requests)
            .isLessThan(5000);
    }
}


