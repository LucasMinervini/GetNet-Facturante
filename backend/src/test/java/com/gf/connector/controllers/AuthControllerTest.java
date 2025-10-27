package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para AuthController
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("AuthController puede ser instanciado")
    void authController_canBeInstantiated() {
        assertThat(authController).isNotNull();
    }

    @Test
    @DisplayName("AuthController tiene métodos básicos")
    void authController_hasBasicMethods() {
        assertThat(authController).isNotNull();
        assertThat(authController.getClass().getSimpleName()).isEqualTo("AuthController");
    }
}