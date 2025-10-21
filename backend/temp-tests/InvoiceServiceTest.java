package com.gf.connector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.domain.Invoice;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.facturante.model.CrearComprobanteRequest;
import com.gf.connector.facturante.model.CrearComprobanteResponse;
import com.gf.connector.facturante.service.FacturanteService;
import com.gf.connector.repo.InvoiceRepository;
import com.gf.connector.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private FacturanteService facturanteService;
    @Mock private ObjectMapper objectMapper;
    @Mock private BillingValidationService validationService;
    @Mock private GetnetToFacturanteTransformationService transformationService;

    @InjectMocks private InvoiceService service;

    private Transaction transaction;
    private BillingValidationService.ValidationResult validResult;
    private BillingValidationService.ValidationResult invalidResult;

    @BeforeEach
    void setup() {
        transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("TXN-123")
                .amount(new BigDecimal("100.00"))
                .status(TransactionStatus.PAID)
                .createdAt(OffsetDateTime.now())
                .build();

        validResult = BillingValidationService.ValidationResult.builder()
                .valid(true)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        invalidResult = BillingValidationService.ValidationResult.builder()
                .valid(false)
                .errors(List.of("Transaction invalid"))
                .warnings(new ArrayList<>())
                .build();
    }

    @Test
    void createPendingInvoice_createsInvoiceWithPendingStatus() {
        String requestJson = "{\"test\": \"data\"}";
        Invoice savedInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .transaction(transaction)
                .status("pending")
                .requestJson(requestJson)
                .build();

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        Invoice result = service.createPendingInvoice(transaction, requestJson);

        assertThat(result.getTransaction()).isEqualTo(transaction);
        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(result.getRequestJson()).isEqualTo(requestJson);
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createFacturaInFacturante_successfulFlow() throws Exception {
        // Arrange
        CrearComprobanteRequest facturanteRequest = new CrearComprobanteRequest();
        CrearComprobanteResponse facturanteResponse = new CrearComprobanteResponse();
        facturanteResponse.setExitoso(true);
        facturanteResponse.setCae("12345678901234");
        facturanteResponse.setNumeroComprobante("0001-00000001");
        facturanteResponse.setPdfUrl("http://example.com/invoice.pdf");

        when(validationService.validateTransaction(transaction)).thenReturn(validResult);
        when(validationService.validateFacturanteRequest(any())).thenReturn(validResult);
        when(transformationService.transformTransactionToFacturanteRequest(any(), any())).thenReturn(facturanteRequest);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"request\": \"data\"}");
        when(facturanteService.crearFactura(transaction)).thenReturn(facturanteResponse);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Invoice result = service.createFacturaInFacturante(transaction);

        // Assert
        assertThat(result.getStatus()).isEqualTo("sent");
        assertThat(result.getPdfUrl()).isEqualTo("http://example.com/invoice.pdf");
        assertThat(transaction.getCae()).isEqualTo("12345678901234");
        assertThat(transaction.getInvoiceNumber()).isEqualTo("0001-00000001");
        assertThat(transaction.getInvoicePdfUrl()).isEqualTo("http://example.com/invoice.pdf");

        verify(validationService).validateTransaction(transaction);
        verify(validationService).validateFacturanteRequest(facturanteRequest);
        verify(transformationService).transformTransactionToFacturanteRequest(transaction, "{}");
        verify(facturanteService).crearFactura(transaction);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void createFacturaInFacturante_invalidTransaction_throwsException() {
        when(validationService.validateTransaction(transaction)).thenReturn(invalidResult);

        assertThatThrownBy(() -> service.createFacturaInFacturante(transaction))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transacción no válida");

        verify(validationService).validateTransaction(transaction);
        verifyNoInteractions(facturanteService);
    }

    @Test
    void createFacturaInFacturante_invalidFacturanteRequest_setsErrorStatus() throws Exception {
        CrearComprobanteRequest facturanteRequest = new CrearComprobanteRequest();
        BillingValidationService.ValidationResult requestValidation = BillingValidationService.ValidationResult.builder()
                .valid(false)
                .errors(List.of("Invalid request"))
                .warnings(new ArrayList<>())
                .build();

        when(validationService.validateTransaction(transaction)).thenReturn(validResult);
        when(transformationService.transformTransactionToFacturanteRequest(any(), any())).thenReturn(facturanteRequest);
        when(validationService.validateFacturanteRequest(facturanteRequest)).thenReturn(requestValidation);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice result = service.createFacturaInFacturante(transaction);

        assertThat(result.getStatus()).isEqualTo("error");
        assertThat(result.getResponseJson()).contains("Validation failed");
        verifyNoInteractions(facturanteService);
    }

    @Test
    void createFacturaInFacturante_facturanteServiceFails_setsErrorStatus() throws Exception {
        CrearComprobanteRequest facturanteRequest = new CrearComprobanteRequest();
        CrearComprobanteResponse facturanteResponse = new CrearComprobanteResponse();
        facturanteResponse.setExitoso(false);

        when(validationService.validateTransaction(transaction)).thenReturn(validResult);
        when(validationService.validateFacturanteRequest(any())).thenReturn(validResult);
        when(transformationService.transformTransactionToFacturanteRequest(any(), any())).thenReturn(facturanteRequest);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"request\": \"data\"}");
        when(facturanteService.crearFactura(transaction)).thenReturn(facturanteResponse);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice result = service.createFacturaInFacturante(transaction);

        assertThat(result.getStatus()).isEqualTo("error");
        verify(facturanteService).crearFactura(transaction);
    }

    @Test
    void createFacturaInFacturante_technicalException_setsErrorStatus() throws Exception {
        CrearComprobanteRequest facturanteRequest = new CrearComprobanteRequest();
        Exception technicalException = new RuntimeException("Technical error");

        when(validationService.validateTransaction(transaction)).thenReturn(validResult);
        when(validationService.validateFacturanteRequest(any())).thenReturn(validResult);
        when(transformationService.transformTransactionToFacturanteRequest(any(), any())).thenReturn(facturanteRequest);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"request\": \"data\"}");
        when(facturanteService.crearFactura(transaction)).thenThrow(technicalException);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\": \"Technical error\"}");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice result = service.createFacturaInFacturante(transaction);

        assertThat(result.getStatus()).isEqualTo("error");
        verify(facturanteService).crearFactura(transaction);
    }

    @Test
    void createFacturaInFacturante_validationException_throwsException() throws Exception {
        when(validationService.validateTransaction(transaction)).thenReturn(invalidResult);

        assertThatThrownBy(() -> service.createFacturaInFacturante(transaction))
                .isInstanceOf(IllegalArgumentException.class);

        verify(validationService).validateTransaction(transaction);
        verifyNoInteractions(facturanteService);
    }

    @Test
    void createFacturaInFacturante_jsonSerializationException_handlesGracefully() throws Exception {
        CrearComprobanteRequest facturanteRequest = new CrearComprobanteRequest();

        when(validationService.validateTransaction(transaction)).thenReturn(validResult);
        when(validationService.validateFacturanteRequest(any())).thenReturn(validResult);
        when(transformationService.transformTransactionToFacturanteRequest(any(), any())).thenReturn(facturanteRequest);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice result = service.createFacturaInFacturante(transaction);

        assertThat(result.getStatus()).isEqualTo("error");
        verifyNoInteractions(facturanteService);
    }
}
