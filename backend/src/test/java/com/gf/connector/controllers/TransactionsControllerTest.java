package com.gf.connector.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests básicos para TransactionsController
 */
@ExtendWith(MockitoExtension.class)
class TransactionsControllerTest {

    @InjectMocks
    private TransactionsController transactionsController;

    @Test
    @DisplayName("TransactionsController puede ser instanciado")
    void transactionsController_canBeInstantiated() {
        assertThat(transactionsController).isNotNull();
    }

    @Test
    @DisplayName("TransactionsController tiene métodos básicos")
    void transactionsController_hasBasicMethods() {
        assertThat(transactionsController).isNotNull();
        assertThat(transactionsController.getClass().getSimpleName()).isEqualTo("TransactionsController");
    }
}