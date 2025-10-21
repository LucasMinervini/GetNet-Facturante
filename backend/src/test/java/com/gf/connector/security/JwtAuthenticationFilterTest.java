package com.gf.connector.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtTokenService jwtTokenService;
    private UserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtTokenService = mock(JwtTokenService.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtAuthenticationFilter(jwtTokenService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenNoAuthorizationHeader_thenChainContinues_withoutAuth() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void whenValidBearerToken_setsAuthentication_andTenantAttribute() throws Exception {
        String token = "abc";
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtTokenService.validateAccessTokenAndGetSubject(token)).thenReturn("user@example.com");
        when(jwtTokenService.getTokenClaims(token)).thenReturn(Map.of(
                "roles", List.of(Map.of("authority", "ROLE_USER"))
        ));
        String tenant = UUID.randomUUID().toString();
        when(jwtTokenService.getTenantIdClaim(token)).thenReturn(tenant);

        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(req, res, chain);

        verify(chain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").contains("ROLE_USER");
        assertThat(req.getAttribute("tenantId")).isNotNull();
    }
}


