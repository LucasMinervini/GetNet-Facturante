package com.gf.connector.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @InjectMocks
    private HealthController healthController;

    @Test
    void testHealth_ReturnsOkStatus() {
        // Act
        Map<String, String> response = healthController.health();

        // Assert
        assertNotNull(response);
        assertEquals("ok", response.get("status"));
        assertEquals(1, response.size());
    }

    @Test
    void testHealth_ResponseIsImmutable() {
        // Act
        Map<String, String> response = healthController.health();

        // Assert
        assertNotNull(response);
        assertThrows(UnsupportedOperationException.class, () -> {
            response.put("test", "value");
        });
    }

    @Test
    void testHealth_ResponseContainsExpectedKey() {
        // Act
        Map<String, String> response = healthController.health();

        // Assert
        assertTrue(response.containsKey("status"));
        assertFalse(response.containsKey("error"));
    }
}
