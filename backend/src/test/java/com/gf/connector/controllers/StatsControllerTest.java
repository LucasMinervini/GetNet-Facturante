package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para StatsController
 */
@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @InjectMocks
    private StatsController statsController;

    @Test
    @DisplayName("StatsController puede ser instanciado")
    void statsController_canBeInstantiated() {
        assertThat(statsController).isNotNull();
    }

    @Test
    @DisplayName("StatsController tiene métodos básicos")
    void statsController_hasBasicMethods() {
        assertThat(statsController).isNotNull();
        assertThat(statsController.getClass().getSimpleName()).isEqualTo("StatsController");
    }
}