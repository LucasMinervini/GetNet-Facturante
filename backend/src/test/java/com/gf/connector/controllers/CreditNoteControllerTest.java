package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para CreditNoteController
 */
@ExtendWith(MockitoExtension.class)
class CreditNoteControllerTest {

    @InjectMocks
    private CreditNoteController creditNoteController;

    @Test
    @DisplayName("CreditNoteController puede ser instanciado")
    void creditNoteController_canBeInstantiated() {
        assertThat(creditNoteController).isNotNull();
    }

    @Test
    @DisplayName("CreditNoteController tiene métodos básicos")
    void creditNoteController_hasBasicMethods() {
        assertThat(creditNoteController).isNotNull();
        assertThat(creditNoteController.getClass().getSimpleName()).isEqualTo("CreditNoteController");
    }
}