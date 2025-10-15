package com.gf.connector.controllers;

import com.gf.connector.domain.Invoice;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private InvoiceController invoiceController;

    private Transaction testTransaction;
    private Invoice testInvoice;
    private UUID testTransactionId;

    @BeforeEach
    void setUp() {
        testTransactionId = UUID.randomUUID();
        
        testTransaction = Transaction.builder()
                .id(testTransactionId)
                .externalId("GETNET-TEST-001")
                .amount(new BigDecimal("150.50"))
                .currency("ARS")
                .status(TransactionStatus.PAID)
                .customerDoc("12345678")
                .capturedAt(OffsetDateTime.now())
                .build();

        testInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .transaction(testTransaction)
                .status("sent")
                .pdfUrl("http://example.com/factura.pdf")
                .build();
    }

    @Test
    void testCreateInvoice_Success() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));
        when(invoiceService.createFacturaInFacturante(any(Transaction.class)))
                .thenReturn(testInvoice);

        // Act
        ResponseEntity<?> response = invoiceController.createInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testInvoice, response.getBody());
        
        verify(transactionRepository).findById(testTransactionId);
        verify(invoiceService).createFacturaInFacturante(testTransaction);
    }

    @Test
    void testCreateInvoice_TransactionNotFound() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = invoiceController.createInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Error al crear factura"));
        assertTrue(response.getBody().toString().contains("Transacción no encontrada"));
        
        verify(transactionRepository).findById(testTransactionId);
        verify(invoiceService, never()).createFacturaInFacturante(any());
    }

    @Test
    void testCreateInvoice_TransactionNotInValidStatus() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.FAILED);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.createInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("La transacción debe estar en estado 'paid' o 'authorized'"));
        
        verify(transactionRepository).findById(testTransactionId);
        verify(invoiceService, never()).createFacturaInFacturante(any());
    }

    @Test
    void testCreateInvoice_TransactionStatusRefunded() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.REFUNDED);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.createInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("La transacción debe estar en estado 'paid' o 'authorized'"));
        
        verify(transactionRepository).findById(testTransactionId);
        verify(invoiceService, never()).createFacturaInFacturante(any());
    }

    @Test
    void testCreateInvoice_TransactionStatusAuthorized() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.AUTHORIZED);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));
        when(invoiceService.createFacturaInFacturante(any(Transaction.class)))
                .thenReturn(testInvoice);

        // Act
        ResponseEntity<?> response = invoiceController.createInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testInvoice, response.getBody());
        
        verify(transactionRepository).findById(testTransactionId);
        verify(invoiceService).createFacturaInFacturante(testTransaction);
    }

    @Test
    void testCreateInvoice_ServiceThrowsException() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));
        when(invoiceService.createFacturaInFacturante(any(Transaction.class)))
                .thenThrow(new RuntimeException("Error en servicio"));

        // Act
        ResponseEntity<?> response = invoiceController.createInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Error al crear factura"));
        assertTrue(response.getBody().toString().contains("Error en servicio"));
        
        verify(transactionRepository).findById(testTransactionId);
        verify(invoiceService).createFacturaInFacturante(testTransaction);
    }

    @Test
    void testGetInvoiceByTransaction_Success() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.getInvoiceByTransaction(testTransactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testTransaction, response.getBody());
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testGetInvoiceByTransaction_TransactionNotFound() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = invoiceController.getInvoiceByTransaction(testTransactionId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Error al obtener factura"));
        assertTrue(response.getBody().toString().contains("Transacción no encontrada"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testGetInvoiceByTransaction_ServiceThrowsException() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenThrow(new RuntimeException("Error de base de datos"));

        // Act
        ResponseEntity<?> response = invoiceController.getInvoiceByTransaction(testTransactionId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Error al obtener factura"));
        assertTrue(response.getBody().toString().contains("Error de base de datos"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testResendInvoice_Success() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.resendInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Factura reenviada exitosamente"));
        assertTrue(response.getBody().toString().contains(testTransactionId.toString()));
        assertTrue(response.getBody().toString().contains("12345678"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testResendInvoice_TransactionNotFound() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = invoiceController.resendInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Error al reemitir factura"));
        assertTrue(response.getBody().toString().contains("Transacción no encontrada"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testResendInvoice_TransactionNotPaid() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.AUTHORIZED);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.resendInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Solo se pueden reenviar facturas de transacciones pagadas"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testResendInvoice_TransactionStatusFailed() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.FAILED);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.resendInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Solo se pueden reenviar facturas de transacciones pagadas"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testResendInvoice_TransactionStatusRefunded() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.REFUNDED);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.resendInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Solo se pueden reenviar facturas de transacciones pagadas"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testResendInvoice_ServiceThrowsException() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenThrow(new RuntimeException("Error de servicio"));

        // Act
        ResponseEntity<?> response = invoiceController.resendInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Error al reemitir factura"));
        assertTrue(response.getBody().toString().contains("Error de servicio"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testDownloadInvoicePdf_Success() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.downloadInvoicePdf(testTransactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("PDF simulado para transacción: GETNET-TEST-001"));
        assertEquals("application/pdf", response.getHeaders().getFirst("Content-Type"));
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("attachment"));
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("factura-GETNET-TEST-001.pdf"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testDownloadInvoicePdf_TransactionNotFound() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = invoiceController.downloadInvoicePdf(testTransactionId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Error al descargar PDF"));
        assertTrue(response.getBody().toString().contains("Transacción no encontrada"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testDownloadInvoicePdf_TransactionNotPaid() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.AUTHORIZED);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.downloadInvoicePdf(testTransactionId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("No hay PDF disponible para esta transacción"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testDownloadInvoicePdf_TransactionStatusFailed() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.FAILED);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.downloadInvoicePdf(testTransactionId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("No hay PDF disponible para esta transacción"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testDownloadInvoicePdf_TransactionStatusRefunded() {
        // Arrange
        testTransaction.setStatus(TransactionStatus.REFUNDED);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.downloadInvoicePdf(testTransactionId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("No hay PDF disponible para esta transacción"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testDownloadInvoicePdf_ServiceThrowsException() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenThrow(new RuntimeException("Error de base de datos"));

        // Act
        ResponseEntity<?> response = invoiceController.downloadInvoicePdf(testTransactionId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Error al descargar PDF"));
        assertTrue(response.getBody().toString().contains("Error de base de datos"));
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testCreateInvoice_WithNullCustomerDoc() {
        // Arrange
        testTransaction.setCustomerDoc(null);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));
        when(invoiceService.createFacturaInFacturante(any(Transaction.class)))
                .thenReturn(testInvoice);

        // Act
        ResponseEntity<?> response = invoiceController.createInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testInvoice, response.getBody());
        
        verify(transactionRepository).findById(testTransactionId);
        verify(invoiceService).createFacturaInFacturante(testTransaction);
    }

    @Test
    void testResendInvoice_WithNullCustomerDoc() {
        // Arrange
        testTransaction.setCustomerDoc(null);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.resendInvoice(testTransactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("Factura reenviada exitosamente"));
        assertTrue(response.getBody().toString().contains("null")); // customerDoc es null
        
        verify(transactionRepository).findById(testTransactionId);
    }

    @Test
    void testDownloadInvoicePdf_WithNullExternalId() {
        // Arrange
        testTransaction.setExternalId(null);
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));

        // Act
        ResponseEntity<?> response = invoiceController.downloadInvoicePdf(testTransactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toString().contains("PDF simulado para transacción: null"));
        assertEquals("application/pdf", response.getHeaders().getFirst("Content-Type"));
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("attachment"));
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("factura-null.pdf"));
        
        verify(transactionRepository).findById(testTransactionId);
    }
}
