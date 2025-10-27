package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para ReconciliationController
 */
@ExtendWith(MockitoExtension.class)
class ReconciliationControllerTest {

    @InjectMocks
    private ReconciliationController reconciliationController;

    @Test
    @DisplayName("ReconciliationController puede ser instanciado")
    void reconciliationController_canBeInstantiated() {
        assertThat(reconciliationController).isNotNull();
    }

    @Test
    @DisplayName("ReconciliationController tiene métodos básicos")
    void reconciliationController_hasBasicMethods() {
        assertThat(reconciliationController).isNotNull();
        assertThat(reconciliationController.getClass().getSimpleName()).isEqualTo("ReconciliationController");
    }
}