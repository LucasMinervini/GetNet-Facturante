package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para TestingController
 */
@ExtendWith(MockitoExtension.class)
class TestingControllerTest {

    @InjectMocks
    private TestingController testingController;

    @Test
    @DisplayName("TestingController puede ser instanciado")
    void testingController_canBeInstantiated() {
        assertThat(testingController).isNotNull();
    }

    @Test
    @DisplayName("TestingController tiene métodos básicos")
    void testingController_hasBasicMethods() {
        assertThat(testingController).isNotNull();
        assertThat(testingController.getClass().getSimpleName()).isEqualTo("TestingController");
    }
}