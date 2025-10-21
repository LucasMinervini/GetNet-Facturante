package com.gf.connector.security;

import com.gf.connector.domain.BillingSettings;
import com.gf.connector.repo.BillingSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetnetAuthenticationServiceTest {

    @Mock
    private BillingSettingsRepository repo;
    @Mock
    private RestTemplate rest;
    private GetnetAuthenticationService service;

    @BeforeEach
    void setup() {
        service = new GetnetAuthenticationService(repo, rest);
        ReflectionTestUtils.setField(service, "environment", "sandbox");
        ReflectionTestUtils.setField(service, "apiKey", "k");
        ReflectionTestUtils.setField(service, "apiSecret", "s");
        ReflectionTestUtils.setField(service, "oauthUrlSandbox", "https://oauth/token");
        ReflectionTestUtils.setField(service, "apiUrlSandbox", "https://api");
    }

    @Test
    void getGetnetAccessToken_cachesToken() {
        UUID tenant = UUID.randomUUID();
        when(repo.findByTenantIdAndActivoTrue(tenant)).thenReturn(Optional.of(BillingSettings.builder().tenantId(tenant).build()));
        
        // Mock que retorna un token usando ParameterizedTypeReference
        when(rest.exchange(anyString(), any(HttpMethod.class), any(), any(org.springframework.core.ParameterizedTypeReference.class)))
            .thenReturn(new ResponseEntity<>(Map.of("access_token", "test-token", "expires_in", 3600), HttpStatus.OK));

        String t1 = service.getGetnetAccessToken(tenant);
        String t2 = service.getGetnetAccessToken(tenant);
        
        // Verificar que se llama solo una vez (cache funciona)
        verify(rest, times(1)).exchange(anyString(), any(HttpMethod.class), any(), any(org.springframework.core.ParameterizedTypeReference.class));
        assertThat(t1).isNotNull();
        assertThat(t2).isNotNull();
    }

    @Test
    void getGetnetAccessToken_returnsNullOnError() {
        UUID tenant = UUID.randomUUID();
        when(repo.findByTenantIdAndActivoTrue(tenant)).thenReturn(Optional.empty());
        
        String result = service.getGetnetAccessToken(tenant);
        
        assertThat(result).isNull();
    }
}


