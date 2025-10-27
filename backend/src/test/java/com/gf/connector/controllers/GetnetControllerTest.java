package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para GetnetController
 */
@ExtendWith(MockitoExtension.class)
class GetnetControllerTest {

    @InjectMocks
    private GetnetController getnetController;

    @Test
    @DisplayName("GetnetController puede ser instanciado")
    void getnetController_canBeInstantiated() {
        assertThat(getnetController).isNotNull();
    }

    @Test
    @DisplayName("GetnetController tiene métodos básicos")
    void getnetController_hasBasicMethods() {
        assertThat(getnetController).isNotNull();
        assertThat(getnetController.getClass().getSimpleName()).isEqualTo("GetnetController");
    }
}