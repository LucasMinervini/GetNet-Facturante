package com.gf.connector.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para NotificationService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Tests")
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Notification service exists and can be instantiated")
    void notificationService_canBeInstantiated() {
        // Assert - Verificar que el servicio se puede instanciar
        assertThat(notificationService).isNotNull();
    }
}