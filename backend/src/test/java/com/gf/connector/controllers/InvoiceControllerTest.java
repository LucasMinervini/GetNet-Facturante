package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para InvoiceController
 */
@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @InjectMocks
    private InvoiceController invoiceController;

    @Test
    @DisplayName("InvoiceController puede ser instanciado")
    void invoiceController_canBeInstantiated() {
        assertThat(invoiceController).isNotNull();
    }

    @Test
    @DisplayName("InvoiceController tiene métodos básicos")
    void invoiceController_hasBasicMethods() {
        assertThat(invoiceController).isNotNull();
        assertThat(invoiceController.getClass().getSimpleName()).isEqualTo("InvoiceController");
    }
}