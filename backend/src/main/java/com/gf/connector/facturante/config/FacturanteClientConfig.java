package com.gf.connector.facturante.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class FacturanteClientConfig {
    
    @Bean
    @ConditionalOnProperty(name = "facturante.production", havingValue = "true", matchIfMissing = false)
    public RestTemplate facturanteRestTemplate() {
        return new RestTemplate();
    }
}
