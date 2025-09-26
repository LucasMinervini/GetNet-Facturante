package com.gf.connector.facturante.service;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.facturante.client.IComprobantesProxy;
import com.gf.connector.facturante.config.FacturanteConfig;
import com.gf.connector.facturante.model.CrearComprobanteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturanteServiceTest {

    @Mock
    private IComprobantesProxy comprobantesProxy;

    @Mock
    private FacturanteConfig facturanteConfig;

    @InjectMocks
    private FacturanteService facturanteService;

    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        // Configurar mock de configuración
        when(facturanteConfig.getEmpresa()).thenReturn("TEST_EMPRESA");
        when(facturanteConfig.getUsuario()).thenReturn("TEST_USER");
        when(facturanteConfig.getPassword()).thenReturn("TEST_PASS");
        when(facturanteConfig.getPrefijo()).thenReturn("0001");
        when(facturanteConfig.getTipoComprobante()).thenReturn("FB");

        // Crear transacción de prueba
        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("GETNET-TEST-001")
                .amount(new BigDecimal("150.50"))
                .currency("ARS")
                .status(TransactionStatus.PAID)
                .customerDoc("12345678")
                .capturedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void testCrearFacturaExitosa() throws Exception {
        // Arrange
        CrearComprobanteResponse mockResponse = new CrearComprobanteResponse();
        mockResponse.setExitoso(true);
        mockResponse.setEstado("Aprobado");
        mockResponse.setCae("12345678901234");
        mockResponse.setNumeroComprobante("0001-00000001");
        mockResponse.setMensajes(new String[]{"Comprobante creado exitosamente"});

        when(comprobantesProxy.crearComprobante(any())).thenReturn(mockResponse);

        // Act
        CrearComprobanteResponse response = facturanteService.crearFactura(testTransaction);

        // Assert
        assertNotNull(response);
        assertTrue(response.getExitoso());
        assertEquals("Aprobado", response.getEstado());
        assertEquals("12345678901234", response.getCae());
        assertEquals("0001-00000001", response.getNumeroComprobante());
    }

    @Test
    void testCrearFacturaConError() throws Exception {
        // Arrange
        CrearComprobanteResponse mockResponse = new CrearComprobanteResponse();
        mockResponse.setExitoso(false);
        mockResponse.setEstado("Error");
        mockResponse.setMensajes(new String[]{"Error de validación", "Datos incompletos"});

        when(comprobantesProxy.crearComprobante(any())).thenReturn(mockResponse);

        // Act
        CrearComprobanteResponse response = facturanteService.crearFactura(testTransaction);

        // Assert
        assertNotNull(response);
        assertFalse(response.getExitoso());
        assertEquals("Error", response.getEstado());
        assertEquals(2, response.getMensajes().length);
    }

    @Test
    void testCrearFacturaConExcepcion() throws Exception {
        // Arrange
        when(comprobantesProxy.crearComprobante(any()))
                .thenThrow(new RuntimeException("Error de conexión"));

        // Act
        CrearComprobanteResponse response = facturanteService.crearFactura(testTransaction);

        // Assert
        assertNotNull(response);
        assertFalse(response.getExitoso());
        assertEquals("Error", response.getEstado());
        assertTrue(response.getMensajes()[0].contains("Error interno"));
    }

    @Test
    void testCrearFacturaConTransactionSinDocumento() throws Exception {
        // Arrange
        testTransaction.setCustomerDoc(null);
        
        CrearComprobanteResponse mockResponse = new CrearComprobanteResponse();
        mockResponse.setExitoso(true);
        mockResponse.setEstado("Aprobado");
        mockResponse.setCae("12345678901234");
        mockResponse.setNumeroComprobante("0001-00000001");

        when(comprobantesProxy.crearComprobante(any())).thenReturn(mockResponse);

        // Act
        CrearComprobanteResponse response = facturanteService.crearFactura(testTransaction);

        // Assert
        assertNotNull(response);
        assertTrue(response.getExitoso());
        // Debería usar "00000000" como documento por defecto
    }
}