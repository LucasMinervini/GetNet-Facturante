package com.gf.connector.controllers;

import com.gf.connector.dto.BillingSettingsDto;
import com.gf.connector.service.BillingSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingSettingsControllerTest {

    @Mock
    private BillingSettingsService billingSettingsService;

    @InjectMocks
    private BillingSettingsController billingSettingsController;

    private BillingSettingsDto testSettingsDto;
    private UUID testSettingsId;

    @BeforeEach
    void setUp() {
        testSettingsId = UUID.randomUUID();
        
        testSettingsDto = new BillingSettingsDto();
        testSettingsDto.setId(testSettingsId);
        testSettingsDto.setCuitEmpresa("20-12345678-9");
        testSettingsDto.setRazonSocialEmpresa("TEST_EMPRESA");
        testSettingsDto.setPuntoVenta("0001");
        testSettingsDto.setTipoComprobante("FB");
        testSettingsDto.setIvaPorDefecto(new java.math.BigDecimal("21.0"));
        testSettingsDto.setFacturarSoloPaid(true);
        testSettingsDto.setConsumidorFinalPorDefecto(false);
        testSettingsDto.setActivo(true);
        testSettingsDto.setDescripcion("Configuración de prueba");
        testSettingsDto.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    void testGetActiveSettings_Success() {
        // Arrange
        when(billingSettingsService.getActiveSettingsDto())
                .thenReturn(Optional.of(testSettingsDto));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getActiveSettings();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        
        verify(billingSettingsService).getActiveSettingsDto();
    }

    @Test
    void testGetActiveSettings_NotFound() {
        // Arrange
        when(billingSettingsService.getActiveSettingsDto())
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getActiveSettings();

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).getActiveSettingsDto();
    }

    @Test
    void testGetAllSettings_Success() {
        // Arrange
        List<BillingSettingsDto> settingsList = Arrays.asList(testSettingsDto);
        when(billingSettingsService.getAllSettings()).thenReturn(settingsList);

        // Act
        ResponseEntity<List<BillingSettingsDto>> response = billingSettingsController.getAllSettings();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testSettingsDto, response.getBody().get(0));
        
        verify(billingSettingsService).getAllSettings();
    }

    @Test
    void testGetAllSettings_EmptyList() {
        // Arrange
        when(billingSettingsService.getAllSettings()).thenReturn(Arrays.asList());

        // Act
        ResponseEntity<List<BillingSettingsDto>> response = billingSettingsController.getAllSettings();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        
        verify(billingSettingsService).getAllSettings();
    }

    @Test
    void testGetSettingsById_Success() {
        // Arrange
        when(billingSettingsService.getSettingsById(testSettingsId))
                .thenReturn(Optional.of(testSettingsDto));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getSettingsById(testSettingsId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        
        verify(billingSettingsService).getSettingsById(testSettingsId);
    }

    @Test
    void testGetSettingsById_NotFound() {
        // Arrange
        when(billingSettingsService.getSettingsById(testSettingsId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getSettingsById(testSettingsId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).getSettingsById(testSettingsId);
    }

    @Test
    void testCreateSettings_Success() {
        // Arrange
        when(billingSettingsService.createSettings(any(BillingSettingsDto.class)))
                .thenReturn(testSettingsDto);

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.createSettings(testSettingsDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        
        verify(billingSettingsService).createSettings(testSettingsDto);
    }

    @Test
    void testCreateSettings_Exception() {
        // Arrange
        when(billingSettingsService.createSettings(any(BillingSettingsDto.class)))
                .thenThrow(new RuntimeException("Error de validación"));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.createSettings(testSettingsDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).createSettings(testSettingsDto);
    }

    @Test
    void testUpdateSettings_Success() {
        // Arrange
        when(billingSettingsService.updateSettings(any(UUID.class), any(BillingSettingsDto.class)))
                .thenReturn(testSettingsDto);

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.updateSettings(testSettingsId, testSettingsDto);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        
        verify(billingSettingsService).updateSettings(testSettingsId, testSettingsDto);
    }

    @Test
    void testUpdateSettings_NotFound() {
        // Arrange
        when(billingSettingsService.updateSettings(any(UUID.class), any(BillingSettingsDto.class)))
                .thenThrow(new IllegalArgumentException("Configuración no encontrada"));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.updateSettings(testSettingsId, testSettingsDto);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).updateSettings(testSettingsId, testSettingsDto);
    }

    @Test
    void testUpdateSettings_Exception() {
        // Arrange
        when(billingSettingsService.updateSettings(any(UUID.class), any(BillingSettingsDto.class)))
                .thenThrow(new RuntimeException("Error de validación"));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.updateSettings(testSettingsId, testSettingsDto);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).updateSettings(testSettingsId, testSettingsDto);
    }

    @Test
    void testActivateSettings_Success() {
        // Arrange
        when(billingSettingsService.activateSettings(testSettingsId))
                .thenReturn(testSettingsDto);

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.activateSettings(testSettingsId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        
        verify(billingSettingsService).activateSettings(testSettingsId);
    }

    @Test
    void testActivateSettings_NotFound() {
        // Arrange
        when(billingSettingsService.activateSettings(testSettingsId))
                .thenThrow(new IllegalArgumentException("Configuración no encontrada"));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.activateSettings(testSettingsId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).activateSettings(testSettingsId);
    }

    @Test
    void testActivateSettings_Exception() {
        // Arrange
        when(billingSettingsService.activateSettings(testSettingsId))
                .thenThrow(new RuntimeException("Error de activación"));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.activateSettings(testSettingsId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).activateSettings(testSettingsId);
    }

    @Test
    void testDeleteSettings_Success() {
        // Arrange
        doNothing().when(billingSettingsService).deleteSettings(testSettingsId);

        // Act
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(testSettingsId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).deleteSettings(testSettingsId);
    }

    @Test
    void testDeleteSettings_NotFound() {
        // Arrange
        doThrow(new IllegalArgumentException("Configuración no encontrada"))
                .when(billingSettingsService).deleteSettings(testSettingsId);

        // Act
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(testSettingsId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).deleteSettings(testSettingsId);
    }

    @Test
    void testDeleteSettings_IllegalStateException() {
        // Arrange
        doThrow(new IllegalStateException("No se puede eliminar la única configuración activa"))
                .when(billingSettingsService).deleteSettings(testSettingsId);

        // Act
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(testSettingsId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).deleteSettings(testSettingsId);
    }

    @Test
    void testDeleteSettings_GeneralException() {
        // Arrange
        doThrow(new RuntimeException("Error interno"))
                .when(billingSettingsService).deleteSettings(testSettingsId);

        // Act
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(testSettingsId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).deleteSettings(testSettingsId);
    }

    @Test
    void testInitDefaultSettings_Success() {
        // Arrange
        doNothing().when(billingSettingsService).createDefaultSettingsIfNotExists();
        when(billingSettingsService.getActiveSettingsDto())
                .thenReturn(Optional.of(testSettingsDto));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.initDefaultSettings();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        
        verify(billingSettingsService).createDefaultSettingsIfNotExists();
        verify(billingSettingsService).getActiveSettingsDto();
    }

    @Test
    void testInitDefaultSettings_NotFound() {
        // Arrange
        doNothing().when(billingSettingsService).createDefaultSettingsIfNotExists();
        when(billingSettingsService.getActiveSettingsDto())
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.initDefaultSettings();

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).createDefaultSettingsIfNotExists();
        verify(billingSettingsService).getActiveSettingsDto();
    }

    @Test
    void testGetSettingsById_WithNullId() {
        // Arrange
        when(billingSettingsService.getSettingsById(null))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getSettingsById(null);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).getSettingsById(null);
    }

    @Test
    void testCreateSettings_WithNullDto() {
        // Arrange
        when(billingSettingsService.createSettings(null))
                .thenThrow(new IllegalArgumentException("DTO no puede ser null"));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.createSettings(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).createSettings(null);
    }

    @Test
    void testUpdateSettings_WithNullId() {
        // Arrange
        when(billingSettingsService.updateSettings(null, any(BillingSettingsDto.class)))
                .thenThrow(new IllegalArgumentException("ID no puede ser null"));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.updateSettings(null, testSettingsDto);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).updateSettings(null, testSettingsDto);
    }

    @Test
    void testActivateSettings_WithNullId() {
        // Arrange
        when(billingSettingsService.activateSettings(null))
                .thenThrow(new IllegalArgumentException("ID no puede ser null"));

        // Act
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.activateSettings(null);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).activateSettings(null);
    }

    @Test
    void testDeleteSettings_WithNullId() {
        // Arrange
        doThrow(new IllegalArgumentException("ID no puede ser null"))
                .when(billingSettingsService).deleteSettings(null);

        // Act
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(null);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(billingSettingsService).deleteSettings(null);
    }
}
