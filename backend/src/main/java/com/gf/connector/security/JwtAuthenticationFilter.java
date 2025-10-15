package com.gf.connector.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtTokenService.validateAccessTokenAndGetSubject(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Usar las autoridades del token en lugar de cargar desde BD
                var claims = jwtTokenService.getTokenClaims(token);
                var roles = (java.util.List<?>) claims.get("roles");
                var authorities = new java.util.ArrayList<org.springframework.security.core.GrantedAuthority>();
                
                if (roles != null) {
                    for (Object role : roles) {
                        if (role instanceof java.util.Map) {
                            var roleMap = (java.util.Map<?, ?>) role;
                            var authority = roleMap.get("authority");
                            if (authority instanceof String) {
                                authorities.add(() -> (String) authority);
                            }
                        }
                    }
                }
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                String tenantId = jwtTokenService.getTenantIdClaim(token);
                if (tenantId != null && !tenantId.trim().isEmpty()) {
                    try {
                        request.setAttribute("tenantId", java.util.UUID.fromString(tenantId));
                        System.out.println("DEBUG: tenantId extraído del token: " + tenantId);
                    } catch (Exception e) {
                        System.out.println("ERROR: No se pudo parsear tenantId: " + tenantId + " - " + e.getMessage());
                    }
                } else {
                    System.out.println("DEBUG: tenantId es null o vacío en el token");
                }
            }
        } catch (Exception e) {
            // Token inválido/expirado: continuar sin autenticar, security chain bloqueará si es requerido
        }

        filterChain.doFilter(request, response);
    }
}


