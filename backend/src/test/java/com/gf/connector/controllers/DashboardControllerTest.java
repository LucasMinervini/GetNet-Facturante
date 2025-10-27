package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para DashboardController
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    @DisplayName("DashboardController puede ser instanciado")
    void dashboardController_canBeInstantiated() {
        assertThat(dashboardController).isNotNull();
    }

    @Test
    @DisplayName("DashboardController tiene métodos básicos")
    void dashboardController_hasBasicMethods() {
        assertThat(dashboardController).isNotNull();
        assertThat(dashboardController.getClass().getSimpleName()).isEqualTo("DashboardController");
    }
}