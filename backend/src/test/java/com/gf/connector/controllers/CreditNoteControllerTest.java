package com.gf.connector.controllers;

import com.gf.connector.domain.CreditNote;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.dto.CreditNoteDto;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.service.CreditNoteService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditNoteControllerTest {

    @Mock
    private CreditNoteService creditNoteService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CreditNoteController creditNoteController;

    private Transaction testTransaction;
    private CreditNote testCreditNote;
    private CreditNoteDto testCreditNoteDto;
    private UUID testTransactionId;
    private UUID testCreditNoteId;

    @BeforeEach
    void setUp() {
        testTransactionId = UUID.randomUUID();
        testCreditNoteId = UUID.randomUUID();
        
        testTransaction = Transaction.builder()
                .id(testTransactionId)
                .externalId("GETNET-123")
                .amount(new BigDecimal("150.50"))
                .currency("ARS")
                .status(TransactionStatus.PAID)
                .customerDoc("12345678")
                .capturedAt(OffsetDateTime.now())
                .build();

        testCreditNote = CreditNote.builder()
                .id(testCreditNoteId)
                .transaction(testTransaction)
                .creditNoteNumber("NC-001")
                .refundReason("Reembolso solicitado por el cliente")
                .status("pending")
                .createdAt(OffsetDateTime.now())
                .build();

        testCreditNoteDto = CreditNoteDto.fromEntity(testCreditNote);
    }

    @Test
    void testProcessRefund_Success() {
        // Arrange
        String refundReason = "Reembolso por defecto del producto";
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString()))
                .thenReturn(testCreditNote);

        // Act
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, refundReason);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService).processRefund(testTransaction, refundReason);
    }

    @Test
    void testProcessRefund_TransactionNotFound() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, "test reason");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService, never()).processRefund(any(), anyString());
    }

    @Test
    void testProcessRefund_IllegalArgumentException() {
        // Arrange
        String errorMessage = "La transacción no puede ser reembolsada";
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString()))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // Act
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, "test reason");

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Transacción no válida para reembolso", responseBody.get("error"));
        assertEquals(errorMessage, responseBody.get("message"));
        
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService).processRefund(testTransaction, "test reason");
    }

    @Test
    void testProcessRefund_GeneralException() {
        // Arrange
        String errorMessage = "Error interno del servicio";
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString()))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, "test reason");

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Error interno del servidor", responseBody.get("error"));
        assertEquals(errorMessage, responseBody.get("message"));
        
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService).processRefund(testTransaction, "test reason");
    }

    @Test
    void testProcessRefund_DefaultRefundReason() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString()))
                .thenReturn(testCreditNote);

        // Act
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, null);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService).processRefund(testTransaction, "Reembolso solicitado por el cliente");
    }

    @Test
    void testGetByTransactionId_Success() {
        // Arrange
        when(creditNoteService.findByTransactionId(testTransactionId))
                .thenReturn(Optional.of(testCreditNote));

        // Act
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByTransactionId(testTransactionId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        
        verify(creditNoteService).findByTransactionId(testTransactionId);
    }

    @Test
    void testGetByTransactionId_NotFound() {
        // Arrange
        when(creditNoteService.findByTransactionId(testTransactionId))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByTransactionId(testTransactionId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(creditNoteService).findByTransactionId(testTransactionId);
    }

    @Test
    void testGetByCreditNoteNumber_Success() {
        // Arrange
        String creditNoteNumber = "NC-001";
        when(creditNoteService.findByCreditNoteNumber(creditNoteNumber))
                .thenReturn(Optional.of(testCreditNote));

        // Act
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByCreditNoteNumber(creditNoteNumber);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        
        verify(creditNoteService).findByCreditNoteNumber(creditNoteNumber);
    }

    @Test
    void testGetByCreditNoteNumber_NotFound() {
        // Arrange
        String creditNoteNumber = "NC-NOTEXISTS";
        when(creditNoteService.findByCreditNoteNumber(creditNoteNumber))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByCreditNoteNumber(creditNoteNumber);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        
        verify(creditNoteService).findByCreditNoteNumber(creditNoteNumber);
    }

    @Test
    void testProcessManualCreditNote_Success() {
        // Arrange
        when(creditNoteService.processManualCreditNote(testCreditNoteId))
                .thenReturn(testCreditNote);

        // Act
        ResponseEntity<?> response = creditNoteController.processManualCreditNote(testCreditNoteId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        
        verify(creditNoteService).processManualCreditNote(testCreditNoteId);
    }

    @Test
    void testProcessManualCreditNote_IllegalArgumentException() {
        // Arrange
        String errorMessage = "La nota de crédito no puede ser procesada";
        when(creditNoteService.processManualCreditNote(testCreditNoteId))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // Act
        ResponseEntity<?> response = creditNoteController.processManualCreditNote(testCreditNoteId);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Nota de crédito no válida para procesamiento", responseBody.get("error"));
        assertEquals(errorMessage, responseBody.get("message"));
        
        verify(creditNoteService).processManualCreditNote(testCreditNoteId);
    }

    @Test
    void testProcessManualCreditNote_GeneralException() {
        // Arrange
        String errorMessage = "Error interno del servicio";
        when(creditNoteService.processManualCreditNote(testCreditNoteId))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        ResponseEntity<?> response = creditNoteController.processManualCreditNote(testCreditNoteId);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Error interno del servidor", responseBody.get("error"));
        assertEquals(errorMessage, responseBody.get("message"));
        
        verify(creditNoteService).processManualCreditNote(testCreditNoteId);
    }

    @Test
    void testDownloadCreditNotePdf_Success() {
        // Act
        ResponseEntity<?> response = creditNoteController.downloadCreditNotePdf(testCreditNoteId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Funcionalidad de descarga de PDF en desarrollo", responseBody.get("message"));
        assertEquals(testCreditNoteId, responseBody.get("creditNoteId"));
    }

    @Test
    void testProcessRefund_WithEmptyRefundReason() {
        // Arrange
        when(transactionRepository.findById(testTransactionId))
                .thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString()))
                .thenReturn(testCreditNote);

        // Act
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, "");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService).processRefund(testTransaction, "");
    }

    @Test
    void testGetByCreditNoteNumber_WithSpecialCharacters() {
        // Arrange
        String creditNoteNumber = "NC-001-2024";
        when(creditNoteService.findByCreditNoteNumber(creditNoteNumber))
                .thenReturn(Optional.of(testCreditNote));

        // Act
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByCreditNoteNumber(creditNoteNumber);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        
        verify(creditNoteService).findByCreditNoteNumber(creditNoteNumber);
    }

    @Test
    void testProcessManualCreditNote_WithNullCreditNoteId() {
        // Arrange
        when(creditNoteService.processManualCreditNote(null))
                .thenThrow(new IllegalArgumentException("ID no puede ser null"));

        // Act
        ResponseEntity<?> response = creditNoteController.processManualCreditNote(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Nota de crédito no válida para procesamiento", responseBody.get("error"));
        assertEquals("ID no puede ser null", responseBody.get("message"));
        
        verify(creditNoteService).processManualCreditNote(null);
    }
}
