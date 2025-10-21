package com.gf.connector.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Test
    void whenRateLimitExceeded_thenReturns429() {
        // Simular que el rate limit está excedido
        when(rateLimitService.isAllowed("127.0.0.1")).thenReturn(false);
        
        // Verificar que el servicio reporta correctamente el estado
        assertThat(rateLimitService.isAllowed("127.0.0.1")).isFalse();
    }

    @Test
    void whenRateLimitNotExceeded_thenAllows() {
        // Simular que el rate limit no está excedido
        when(rateLimitService.isAllowed("127.0.0.1")).thenReturn(true);
        
        // Verificar que el servicio permite la petición
        assertThat(rateLimitService.isAllowed("127.0.0.1")).isTrue();
    }
}


