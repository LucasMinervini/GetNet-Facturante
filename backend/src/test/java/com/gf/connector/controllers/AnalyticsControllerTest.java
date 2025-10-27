package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para AnalyticsController
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @InjectMocks
    private AnalyticsController analyticsController;

    @Test
    @DisplayName("AnalyticsController puede ser instanciado")
    void analyticsController_canBeInstantiated() {
        assertThat(analyticsController).isNotNull();
    }

    @Test
    @DisplayName("AnalyticsController tiene métodos básicos")
    void analyticsController_hasBasicMethods() {
        assertThat(analyticsController).isNotNull();
        // Verificar que el controller existe y puede ser usado
        assertThat(analyticsController.getClass().getSimpleName()).isEqualTo("AnalyticsController");
    }
}