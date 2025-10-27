package com.gf.connector.service;

import com.gf.connector.domain.BillingSettings;
import com.gf.connector.repo.BillingSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests unitarios para BackupService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Backup Service Tests")
class BackupServiceTest {

    @Mock
    private BillingSettingsRepository billingSettingsRepository;
    
    @InjectMocks
    private BackupService backupService;
    
    private BillingSettings testSettings;

    @BeforeEach
    void setUp() {
        testSettings = BillingSettings.builder()
                .id(UUID.randomUUID())
                .tenantId(UUID.randomUUID())
                .cuitEmpresa("20123456789")
                .razonSocialEmpresa("Empresa Test")
                .ivaPorDefecto(new BigDecimal("21.00"))
                .tipoComprobante("FB")
                .puntoVenta("0001")
                .activo(true)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Backup service exists and can be instantiated")
    void backupService_canBeInstantiated() {
        // Assert - Verificar que el servicio se puede instanciar
        assertThat(backupService).isNotNull();
    }
}
