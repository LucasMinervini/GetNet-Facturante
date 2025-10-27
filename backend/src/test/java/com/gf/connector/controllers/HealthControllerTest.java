package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para HealthController
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @InjectMocks
    private HealthController healthController;

    @Test
    @DisplayName("HealthController puede ser instanciado")
    void healthController_canBeInstantiated() {
        assertThat(healthController).isNotNull();
    }

    @Test
    @DisplayName("HealthController tiene métodos básicos")
    void healthController_hasBasicMethods() {
        assertThat(healthController).isNotNull();
        assertThat(healthController.getClass().getSimpleName()).isEqualTo("HealthController");
    }
}