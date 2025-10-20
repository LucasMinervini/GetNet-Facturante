package com.gf.connector.security;

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
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private RateLimitService rateLimitService;

    private String bearer(String token) { return "Bearer " + token; }

    @Test
    void whenRateLimitExceeded_thenReturns429() throws Exception {
        String token = "rl-user";
        java.util.List<java.util.Map<String, String>> roleMaps = java.util.List.of(java.util.Map.of("authority", "ROLE_USER"));
        when(jwtTokenService.validateAccessTokenAndGetSubject(token)).thenReturn("user@example.com");
        when(jwtTokenService.getTenantIdClaim(token)).thenReturn(java.util.UUID.randomUUID().toString());
        when(jwtTokenService.getTokenClaims(token)).thenReturn(java.util.Map.of("roles", roleMaps));

        // Forzar denegación por rate-limit
        when(rateLimitService.isAllowed(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        mockMvc.perform(get("/api/transactions").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isTooManyRequests());
    }
}


