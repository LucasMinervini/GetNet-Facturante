package com.gf.connector.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> meterRegistryCommonTags() {
        return registry -> registry.config().commonTags("app", "getnet-facturante");
    }

    @Bean
    public AtomicInteger liveUsersGauge(MeterRegistry registry) {
        AtomicInteger value = new AtomicInteger(0);
        Gauge.builder("app_live_users", value, AtomicInteger::get)
                .description("Usuarios activos en la aplicación")
                .register(registry);
        return value;
    }
}


