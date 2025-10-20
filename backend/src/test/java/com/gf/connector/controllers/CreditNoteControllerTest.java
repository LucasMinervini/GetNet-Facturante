package com.gf.connector.controllers;

import com.gf.connector.controllers.CreditNoteController;
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

    @Mock private CreditNoteService creditNoteService;
    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private CreditNoteController creditNoteController;

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
        String refundReason = "Reembolso por defecto del producto";
        when(transactionRepository.findById(testTransactionId)).thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString())).thenReturn(testCreditNote);
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, refundReason);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService).processRefund(testTransaction, refundReason);
    }

    @Test
    void testProcessRefund_TransactionNotFound() {
        when(transactionRepository.findById(testTransactionId)).thenReturn(Optional.empty());
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, "test reason");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService, never()).processRefund(any(), anyString());
    }

    @Test
    void testProcessRefund_IllegalArgumentException() {
        String errorMessage = "La transacción no puede ser reembolsada";
        when(transactionRepository.findById(testTransactionId)).thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString())).thenThrow(new IllegalArgumentException(errorMessage));
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, "test reason");
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
        String errorMessage = "Error interno del servicio";
        when(transactionRepository.findById(testTransactionId)).thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString())).thenThrow(new RuntimeException(errorMessage));
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, "test reason");
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
        when(transactionRepository.findById(testTransactionId)).thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString())).thenReturn(testCreditNote);
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService).processRefund(testTransaction, "Reembolso solicitado por el cliente");
    }

    @Test
    void testGetByTransactionId_Success() {
        when(creditNoteService.findByTransactionId(testTransactionId)).thenReturn(Optional.of(testCreditNote));
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByTransactionId(testTransactionId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        verify(creditNoteService).findByTransactionId(testTransactionId);
    }

    @Test
    void testGetByTransactionId_NotFound() {
        when(creditNoteService.findByTransactionId(testTransactionId)).thenReturn(Optional.empty());
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByTransactionId(testTransactionId);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(creditNoteService).findByTransactionId(testTransactionId);
    }

    @Test
    void testGetByCreditNoteNumber_Success() {
        String creditNoteNumber = "NC-001";
        when(creditNoteService.findByCreditNoteNumber(creditNoteNumber)).thenReturn(Optional.of(testCreditNote));
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByCreditNoteNumber(creditNoteNumber);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        verify(creditNoteService).findByCreditNoteNumber(creditNoteNumber);
    }

    @Test
    void testGetByCreditNoteNumber_NotFound() {
        String creditNoteNumber = "NC-NOTEXISTS";
        when(creditNoteService.findByCreditNoteNumber(creditNoteNumber)).thenReturn(Optional.empty());
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByCreditNoteNumber(creditNoteNumber);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(creditNoteService).findByCreditNoteNumber(creditNoteNumber);
    }

    @Test
    void testProcessManualCreditNote_Success() {
        when(creditNoteService.processManualCreditNote(testCreditNoteId)).thenReturn(testCreditNote);
        ResponseEntity<?> response = creditNoteController.processManualCreditNote(testCreditNoteId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        verify(creditNoteService).processManualCreditNote(testCreditNoteId);
    }

    @Test
    void testProcessManualCreditNote_IllegalArgumentException() {
        String errorMessage = "La nota de crédito no puede ser procesada";
        when(creditNoteService.processManualCreditNote(testCreditNoteId)).thenThrow(new IllegalArgumentException(errorMessage));
        ResponseEntity<?> response = creditNoteController.processManualCreditNote(testCreditNoteId);
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
        String errorMessage = "Error interno del servicio";
        when(creditNoteService.processManualCreditNote(testCreditNoteId)).thenThrow(new RuntimeException(errorMessage));
        ResponseEntity<?> response = creditNoteController.processManualCreditNote(testCreditNoteId);
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
        ResponseEntity<?> response = creditNoteController.downloadCreditNotePdf(testCreditNoteId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testProcessRefund_WithEmptyRefundReason() {
        when(transactionRepository.findById(testTransactionId)).thenReturn(Optional.of(testTransaction));
        when(creditNoteService.processRefund(any(Transaction.class), anyString())).thenReturn(testCreditNote);
        ResponseEntity<?> response = creditNoteController.processRefund(testTransactionId, "");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        verify(transactionRepository).findById(testTransactionId);
        verify(creditNoteService).processRefund(testTransaction, "");
    }

    @Test
    void testGetByCreditNoteNumber_WithSpecialCharacters() {
        String creditNoteNumber = "NC-001-2024";
        when(creditNoteService.findByCreditNoteNumber(creditNoteNumber)).thenReturn(Optional.of(testCreditNote));
        ResponseEntity<CreditNoteDto> response = creditNoteController.getByCreditNoteNumber(creditNoteNumber);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCreditNoteDto, response.getBody());
        verify(creditNoteService).findByCreditNoteNumber(creditNoteNumber);
    }

    @Test
    void testProcessManualCreditNote_WithNullCreditNoteId() {
        when(creditNoteService.processManualCreditNote(null)).thenThrow(new IllegalArgumentException("ID no puede ser null"));
        ResponseEntity<?> response = creditNoteController.processManualCreditNote(null);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("Nota de crédito no válida para procesamiento", responseBody.get("error"));
        assertEquals("ID no puede ser null", responseBody.get("message"));
        verify(creditNoteService).processManualCreditNote(null);
    }
}


