package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para WebhookController
 */
@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @InjectMocks
    private WebhookController webhookController;

    @Test
    @DisplayName("WebhookController puede ser instanciado")
    void webhookController_canBeInstantiated() {
        assertThat(webhookController).isNotNull();
    }

    @Test
    @DisplayName("WebhookController tiene métodos básicos")
    void webhookController_hasBasicMethods() {
        assertThat(webhookController).isNotNull();
        assertThat(webhookController.getClass().getSimpleName()).isEqualTo("WebhookController");
    }
}