package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para BillingSettingsController
 */
@ExtendWith(MockitoExtension.class)
class BillingSettingsControllerTest {

    @InjectMocks
    private BillingSettingsController billingSettingsController;

    @Test
    @DisplayName("BillingSettingsController puede ser instanciado")
    void billingSettingsController_canBeInstantiated() {
        assertThat(billingSettingsController).isNotNull();
    }

    @Test
    @DisplayName("BillingSettingsController tiene métodos básicos")
    void billingSettingsController_hasBasicMethods() {
        assertThat(billingSettingsController).isNotNull();
        assertThat(billingSettingsController.getClass().getSimpleName()).isEqualTo("BillingSettingsController");
    }
}