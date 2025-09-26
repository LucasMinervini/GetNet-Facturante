package com.gf.connector.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.domain.Invoice;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.security.GetnetSignatureService;
import com.gf.connector.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private WebhookService webhookService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private GetnetSignatureService signatureService;

    @InjectMocks
    private WebhookController webhookController;

    private MockHttpServletRequest mockRequest;
    private String testRawBody;
    private Map<String, Object> testPayload;
    private Transaction testTransaction;
    private Invoice testInvoice;
    private WebhookService.WebhookProcessingResult successResult;
    private WebhookService.WebhookProcessingResult errorResult;
    private TypeReference<Map<String, Object>> typeRef;

    @BeforeEach
    void setUp() throws Exception {
        mockRequest = new MockHttpServletRequest();
        testRawBody = "{\"transaction_id\":\"GETNET-123\",\"status\":\"paid\"}";
        
        testPayload = Map.of(
                "transaction_id", "GETNET-123",
                "status", "paid"
        );

        testTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("GETNET-123")
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

        successResult = WebhookService.WebhookProcessingResult.builder()
                .success(true)
                .message("Webhook procesado exitosamente")
                .transaction(testTransaction)
                .invoice(testInvoice)
                .build();

        errorResult = WebhookService.WebhookProcessingResult.builder()
                .success(false)
                .message("Error al procesar webhook")
                .transaction(testTransaction)
                .build();

        typeRef = new TypeReference<Map<String, Object>>(){};
    }

    @Test
    void testHandleGetnet_Success() throws Exception {
        // Arrange
        when(signatureService.getSignatureHeaderName()).thenReturn("X-Getnet-Signature");
        when(signatureService.verify(anyString(), anyString())).thenReturn(true);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(testPayload);
        when(webhookService.processGetnetPayload(anyString(), any())).thenReturn(successResult);

        mockRequest.addHeader("X-Getnet-Signature", "valid-signature");

        // Act
        ResponseEntity<?> response = webhookController.handleGetnet(testRawBody, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("success", responseBody.get("status"));
        assertEquals(true, responseBody.get("received"));
        assertEquals("Webhook procesado exitosamente", responseBody.get("message"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> debug = (Map<String, Object>) responseBody.get("debug");
        assertNotNull(debug);
        assertEquals(testTransaction.getId(), debug.get("transaction_id"));
        assertEquals(testTransaction.getExternalId(), debug.get("external_id"));
        assertEquals(testTransaction.getStatus(), debug.get("transaction_status"));
        assertEquals(true, debug.get("invoice_generated"));
        assertEquals(testInvoice.getId(), debug.get("invoice_id"));
        assertEquals(testInvoice.getStatus(), debug.get("invoice_status"));
        
        verify(signatureService).getSignatureHeaderName();
        verify(signatureService).verify(testRawBody, "valid-signature");
        verify(objectMapper).readValue(eq(testRawBody), any(TypeReference.class));
        verify(webhookService).processGetnetPayload(testRawBody, testPayload);
    }

    @Test
    void testHandleGetnet_InvalidSignature() throws Exception {
        // Arrange
        when(signatureService.getSignatureHeaderName()).thenReturn("X-Getnet-Signature");
        when(signatureService.verify(anyString(), anyString())).thenReturn(false);

        mockRequest.addHeader("X-Getnet-Signature", "invalid-signature");

        // Act
        ResponseEntity<?> response = webhookController.handleGetnet(testRawBody, mockRequest);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("error", responseBody.get("status"));
        assertEquals(false, responseBody.get("received"));
        assertEquals("invalid_signature", responseBody.get("error"));
        assertEquals("INVALID_SIGNATURE", responseBody.get("error_code"));
        assertTrue(responseBody.get("hint").toString().contains("X-Getnet-Signature"));
        
        verify(signatureService).getSignatureHeaderName();
        verify(signatureService).verify(testRawBody, "invalid-signature");
        verify(objectMapper, never()).readValue(anyString(), any(TypeReference.class));
        verify(webhookService, never()).processGetnetPayload(anyString(), any());
    }

    @Test
    void testHandleGetnet_MissingSignature() throws Exception {
        // Arrange
        when(signatureService.getSignatureHeaderName()).thenReturn("X-Getnet-Signature");
        when(signatureService.verify(anyString(), anyString())).thenReturn(false);

        // No agregar header de firma

        // Act
        ResponseEntity<?> response = webhookController.handleGetnet(testRawBody, mockRequest);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("error", responseBody.get("status"));
        assertEquals(false, responseBody.get("received"));
        assertEquals("invalid_signature", responseBody.get("error"));
        
        verify(signatureService).getSignatureHeaderName();
        verify(signatureService).verify(testRawBody, null);
        verify(objectMapper, never()).readValue(anyString(), any(TypeReference.class));
        verify(webhookService, never()).processGetnetPayload(anyString(), any());
    }

    @Test
    void testHandleGetnet_ProcessingError() throws Exception {
        // Arrange
        when(signatureService.getSignatureHeaderName()).thenReturn("X-Getnet-Signature");
        when(signatureService.verify(anyString(), anyString())).thenReturn(true);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(testPayload);
        when(webhookService.processGetnetPayload(anyString(), any())).thenReturn(errorResult);

        mockRequest.addHeader("X-Getnet-Signature", "valid-signature");

        // Act
        ResponseEntity<?> response = webhookController.handleGetnet(testRawBody, mockRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("error", responseBody.get("status"));
        assertEquals(false, responseBody.get("received"));
        assertEquals("Error parsing or processing webhook: null", responseBody.get("message"));
        assertEquals("PARSING_ERROR", responseBody.get("error_code"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> debug = (Map<String, Object>) responseBody.get("debug");
        assertNotNull(debug);
        assertEquals(testTransaction.getId(), debug.get("transaction_id"));
        assertNull(debug.get("webhook_event_id"));
        
        verify(signatureService).getSignatureHeaderName();
        verify(signatureService).verify(testRawBody, "valid-signature");
        verify(objectMapper).readValue(eq(testRawBody), any(TypeReference.class));
        verify(webhookService).processGetnetPayload(testRawBody, testPayload);
    }

    @Test
    void testHandleGetnet_JsonParsingError() throws Exception {
        // Arrange
        when(signatureService.getSignatureHeaderName()).thenReturn("X-Getnet-Signature");
        when(signatureService.verify(anyString(), anyString())).thenReturn(true);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenThrow(new RuntimeException("Invalid JSON"));

        mockRequest.addHeader("X-Getnet-Signature", "valid-signature");

        // Act
        ResponseEntity<?> response = webhookController.handleGetnet(testRawBody, mockRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("error", responseBody.get("status"));
        assertEquals(false, responseBody.get("received"));
        assertTrue(responseBody.get("message").toString().contains("Error parsing or processing webhook"));
        assertEquals("PARSING_ERROR", responseBody.get("error_code"));
        
        verify(signatureService).getSignatureHeaderName();
        verify(signatureService).verify(testRawBody, "valid-signature");
        verify(objectMapper).readValue(eq(testRawBody), any(TypeReference.class));
        verify(webhookService, never()).processGetnetPayload(anyString(), any());
    }

    @Test
    void testHandleGetnet_ServiceThrowsException() throws Exception {
        // Arrange
        when(signatureService.getSignatureHeaderName()).thenReturn("X-Getnet-Signature");
        when(signatureService.verify(anyString(), anyString())).thenReturn(true);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(testPayload);
        when(webhookService.processGetnetPayload(anyString(), any())).thenThrow(new RuntimeException("Service error"));

        mockRequest.addHeader("X-Getnet-Signature", "valid-signature");

        // Act
        ResponseEntity<?> response = webhookController.handleGetnet(testRawBody, mockRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("error", responseBody.get("status"));
        assertEquals(false, responseBody.get("received"));
        assertTrue(responseBody.get("message").toString().contains("Error parsing or processing webhook"));
        assertEquals("PARSING_ERROR", responseBody.get("error_code"));
        
        verify(signatureService).getSignatureHeaderName();
        verify(signatureService).verify(testRawBody, "valid-signature");
        verify(objectMapper).readValue(eq(testRawBody), any(TypeReference.class));
        verify(webhookService).processGetnetPayload(testRawBody, testPayload);
    }

    @Test
    void testHandleGetnet_EmptyBody() throws Exception {
        // Arrange
        String emptyBody = "";
        when(signatureService.getSignatureHeaderName()).thenReturn("X-Getnet-Signature");
        when(signatureService.verify(anyString(), anyString())).thenReturn(true);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenThrow(new RuntimeException("Empty body"));

        mockRequest.addHeader("X-Getnet-Signature", "valid-signature");

        // Act
        ResponseEntity<?> response = webhookController.handleGetnet(emptyBody, mockRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("error", responseBody.get("status"));
        assertEquals(false, responseBody.get("received"));
        assertTrue(responseBody.get("message").toString().contains("Error parsing or processing webhook"));
        assertEquals("PARSING_ERROR", responseBody.get("error_code"));
        
        verify(signatureService).getSignatureHeaderName();
        verify(signatureService).verify(emptyBody, "valid-signature");
        verify(objectMapper).readValue(eq(emptyBody), any(TypeReference.class));
        verify(webhookService, never()).processGetnetPayload(anyString(), any());
    }

    @Test
    void testHandleGetnet_NullBody() throws Exception {
        // Arrange
        String nullBody = null;
        when(signatureService.getSignatureHeaderName()).thenReturn("X-Getnet-Signature");
        when(signatureService.verify(anyString(), anyString())).thenReturn(true);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenThrow(new RuntimeException("Null body"));

        mockRequest.addHeader("X-Getnet-Signature", "valid-signature");

        // Act
        ResponseEntity<?> response = webhookController.handleGetnet(nullBody, mockRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("error", responseBody.get("status"));
        assertEquals(false, responseBody.get("received"));
        assertTrue(responseBody.get("message").toString().contains("Error parsing or processing webhook"));
        assertEquals("PARSING_ERROR", responseBody.get("error_code"));
        
        verify(signatureService).getSignatureHeaderName();
        verify(signatureService).verify(nullBody, "valid-signature");
        verify(objectMapper).readValue(eq(nullBody), any(TypeReference.class));
        verify(webhookService, never()).processGetnetPayload(anyString(), any());
    }

    @Test
    void testHandleGetnet_DifferentSignatureHeader() throws Exception {
        // Arrange
        when(signatureService.getSignatureHeaderName()).thenReturn("X-Custom-Signature");
        when(signatureService.verify(anyString(), anyString())).thenReturn(true);
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(testPayload);
        when(webhookService.processGetnetPayload(anyString(), any())).thenReturn(successResult);

        mockRequest.addHeader("X-Custom-Signature", "valid-signature");

        // Act
        ResponseEntity<?> response = webhookController.handleGetnet(testRawBody, mockRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertEquals("success", responseBody.get("status"));
        
        verify(signatureService).getSignatureHeaderName();
        verify(signatureService).verify(testRawBody, "valid-signature");
        verify(objectMapper).readValue(eq(testRawBody), any(TypeReference.class));
        verify(webhookService).processGetnetPayload(testRawBody, testPayload);
    }
}
