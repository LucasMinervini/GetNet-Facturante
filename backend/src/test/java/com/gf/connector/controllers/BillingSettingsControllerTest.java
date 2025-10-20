package com.gf.connector.controllers;

import com.gf.connector.controllers.BillingSettingsController;
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
        when(billingSettingsService.getActiveSettingsDto()).thenReturn(Optional.of(testSettingsDto));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getActiveSettings();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        verify(billingSettingsService).getActiveSettingsDto();
    }

    @Test
    void testGetActiveSettings_NotFound() {
        when(billingSettingsService.getActiveSettingsDto()).thenReturn(Optional.empty());
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getActiveSettings();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).getActiveSettingsDto();
    }

    @Test
    void testGetAllSettings_Success() {
        List<BillingSettingsDto> settingsList = Arrays.asList(testSettingsDto);
        when(billingSettingsService.getAllSettings()).thenReturn(settingsList);
        ResponseEntity<List<BillingSettingsDto>> response = billingSettingsController.getAllSettings();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testSettingsDto, response.getBody().get(0));
        verify(billingSettingsService).getAllSettings();
    }

    @Test
    void testGetAllSettings_EmptyList() {
        when(billingSettingsService.getAllSettings()).thenReturn(Arrays.asList());
        ResponseEntity<List<BillingSettingsDto>> response = billingSettingsController.getAllSettings();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(billingSettingsService).getAllSettings();
    }

    @Test
    void testGetSettingsById_Success() {
        when(billingSettingsService.getSettingsById(testSettingsId)).thenReturn(Optional.of(testSettingsDto));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getSettingsById(testSettingsId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        verify(billingSettingsService).getSettingsById(testSettingsId);
    }

    @Test
    void testGetSettingsById_NotFound() {
        when(billingSettingsService.getSettingsById(testSettingsId)).thenReturn(Optional.empty());
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getSettingsById(testSettingsId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).getSettingsById(testSettingsId);
    }

    @Test
    void testCreateSettings_Success() {
        when(billingSettingsService.createSettings(any(BillingSettingsDto.class))).thenReturn(testSettingsDto);
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.createSettings(testSettingsDto);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        verify(billingSettingsService).createSettings(testSettingsDto);
    }

    @Test
    void testCreateSettings_Exception() {
        when(billingSettingsService.createSettings(any(BillingSettingsDto.class))).thenThrow(new RuntimeException("Error de validación"));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.createSettings(testSettingsDto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).createSettings(testSettingsDto);
    }

    @Test
    void testUpdateSettings_Success() {
        when(billingSettingsService.updateSettings(any(UUID.class), any(BillingSettingsDto.class))).thenReturn(testSettingsDto);
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.updateSettings(testSettingsId, testSettingsDto);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        verify(billingSettingsService).updateSettings(testSettingsId, testSettingsDto);
    }

    @Test
    void testUpdateSettings_NotFound() {
        when(billingSettingsService.updateSettings(any(UUID.class), any(BillingSettingsDto.class))).thenThrow(new IllegalArgumentException("Configuración no encontrada"));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.updateSettings(testSettingsId, testSettingsDto);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).updateSettings(testSettingsId, testSettingsDto);
    }

    @Test
    void testUpdateSettings_Exception() {
        when(billingSettingsService.updateSettings(any(UUID.class), any(BillingSettingsDto.class))).thenThrow(new RuntimeException("Error de validación"));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.updateSettings(testSettingsId, testSettingsDto);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).updateSettings(testSettingsId, testSettingsDto);
    }

    @Test
    void testActivateSettings_Success() {
        when(billingSettingsService.activateSettings(testSettingsId)).thenReturn(testSettingsDto);
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.activateSettings(testSettingsId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        verify(billingSettingsService).activateSettings(testSettingsId);
    }

    @Test
    void testActivateSettings_NotFound() {
        when(billingSettingsService.activateSettings(testSettingsId)).thenThrow(new IllegalArgumentException("Configuración no encontrada"));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.activateSettings(testSettingsId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).activateSettings(testSettingsId);
    }

    @Test
    void testActivateSettings_Exception() {
        when(billingSettingsService.activateSettings(testSettingsId)).thenThrow(new RuntimeException("Error de activación"));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.activateSettings(testSettingsId);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).activateSettings(testSettingsId);
    }

    @Test
    void testDeleteSettings_Success() {
        doNothing().when(billingSettingsService).deleteSettings(testSettingsId);
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(testSettingsId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).deleteSettings(testSettingsId);
    }

    @Test
    void testDeleteSettings_NotFound() {
        doThrow(new IllegalArgumentException("Configuración no encontrada")).when(billingSettingsService).deleteSettings(testSettingsId);
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(testSettingsId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).deleteSettings(testSettingsId);
    }

    @Test
    void testDeleteSettings_IllegalStateException() {
        doThrow(new IllegalStateException("No se puede eliminar la única configuración activa")).when(billingSettingsService).deleteSettings(testSettingsId);
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(testSettingsId);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).deleteSettings(testSettingsId);
    }

    @Test
    void testDeleteSettings_GeneralException() {
        doThrow(new RuntimeException("Error interno")).when(billingSettingsService).deleteSettings(testSettingsId);
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(testSettingsId);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).deleteSettings(testSettingsId);
    }

    @Test
    void testInitDefaultSettings_Success() {
        doNothing().when(billingSettingsService).createDefaultSettingsIfNotExists();
        when(billingSettingsService.getActiveSettingsDto()).thenReturn(Optional.of(testSettingsDto));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.initDefaultSettings();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSettingsDto, response.getBody());
        verify(billingSettingsService).createDefaultSettingsIfNotExists();
        verify(billingSettingsService).getActiveSettingsDto();
    }

    @Test
    void testInitDefaultSettings_NotFound() {
        doNothing().when(billingSettingsService).createDefaultSettingsIfNotExists();
        when(billingSettingsService.getActiveSettingsDto()).thenReturn(Optional.empty());
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.initDefaultSettings();
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).createDefaultSettingsIfNotExists();
        verify(billingSettingsService).getActiveSettingsDto();
    }

    @Test
    void testGetSettingsById_WithNullId() {
        when(billingSettingsService.getSettingsById(null)).thenReturn(Optional.empty());
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.getSettingsById(null);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).getSettingsById(null);
    }

    @Test
    void testCreateSettings_WithNullDto() {
        when(billingSettingsService.createSettings(null)).thenThrow(new IllegalArgumentException("DTO no puede ser null"));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.createSettings(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).createSettings(null);
    }

    @Test
    void testUpdateSettings_WithNullId() {
        when(billingSettingsService.updateSettings(null, any(BillingSettingsDto.class))).thenThrow(new IllegalArgumentException("ID no puede ser null"));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.updateSettings(null, testSettingsDto);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).updateSettings(null, testSettingsDto);
    }

    @Test
    void testActivateSettings_WithNullId() {
        when(billingSettingsService.activateSettings(null)).thenThrow(new IllegalArgumentException("ID no puede ser null"));
        ResponseEntity<BillingSettingsDto> response = billingSettingsController.activateSettings(null);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).activateSettings(null);
    }

    @Test
    void testDeleteSettings_WithNullId() {
        doThrow(new IllegalArgumentException("ID no puede ser null")).when(billingSettingsService).deleteSettings(null);
        ResponseEntity<Void> response = billingSettingsController.deleteSettings(null);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(billingSettingsService).deleteSettings(null);
    }
}


