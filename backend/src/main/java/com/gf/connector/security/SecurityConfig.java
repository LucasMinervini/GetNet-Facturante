package com.gf.connector.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.gf.connector.service.UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public SecurityConfig() {}

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter, RateLimitFilter rateLimitFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Auth y Webhooks (públicos)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/actuator/health", "/health").permitAll()
                .requestMatchers("/api/health").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Swagger/OpenAPI (solo en desarrollo)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Transactions (lectura para USER+, acciones sensibles ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/transactions/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/transactions/reset-error-to-pending").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/transactions/initialize-billing-status").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/transactions/create-test-data").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/transactions/setup-test-data").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/transactions/*/confirm-billing").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/transactions/test-getnet-connection").hasRole("ADMIN")

                // Invoices (crear/reemitir ADMIN, lectura/pdf USER+)
                .requestMatchers(HttpMethod.POST, "/api/invoices/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/invoices/**").hasAnyRole("USER", "ADMIN")

                // Billing settings (lectura USER+, escritura ADMIN)
                .requestMatchers(HttpMethod.GET, "/api/billing-settings/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/billing-settings/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/billing-settings/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/billing-settings/**").hasRole("ADMIN")
                
                // Credit Notes (ADMIN only)
                .requestMatchers("/api/credit-notes/**").hasRole("ADMIN")

                // Cualquier otro endpoint autenticado por defecto
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            );

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenService jwtTokenService, UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtTokenService, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserService userService) {
        return userService;
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }
}


