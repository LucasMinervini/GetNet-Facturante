package com.gf.connector.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    private RateLimitService service;

    @BeforeEach
    void setup() {
        service = new RateLimitService();
        ReflectionTestUtils.setField(service, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(service, "requestsPerMinute", 5);
    }

    @Test
    void allows_first_requests_then_blocks() {
        String ip = "1.2.3.4";
        for (int i = 0; i < 5; i++) {
            assertThat(service.isAllowed(ip)).isTrue();
        }
        assertThat(service.isAllowed(ip)).isFalse();
        assertThat(service.getRemainingRequests(ip)).isEqualTo(0);
    }
}


