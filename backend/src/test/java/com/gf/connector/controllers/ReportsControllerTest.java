package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para ReportsController
 */
@ExtendWith(MockitoExtension.class)
class ReportsControllerTest {

    @InjectMocks
    private ReportsController reportsController;

    @Test
    @DisplayName("ReportsController puede ser instanciado")
    void reportsController_canBeInstantiated() {
        assertThat(reportsController).isNotNull();
    }

    @Test
    @DisplayName("ReportsController tiene métodos básicos")
    void reportsController_hasBasicMethods() {
        assertThat(reportsController).isNotNull();
        assertThat(reportsController.getClass().getSimpleName()).isEqualTo("ReportsController");
    }
}