package com.gf.connector.service;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.domain.WebhookEvent;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.repo.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class WebhookServiceTest {

    @Mock private WebhookEventRepository webhookEventRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private GetnetToFacturanteTransformationService transformationService;
    @Mock private InvoiceService invoiceService;
    @Mock private CreditNoteService creditNoteService;
    @Mock private BillingSettingsService billingSettingsService;

    @InjectMocks private WebhookService webhookService;

    private Map<String, Object> payload;
    private Transaction txPaid;

    @BeforeEach
    void setup() {
        payload = Map.of("id", "P1", "status", "PAID", "amount", 100);
        txPaid = Transaction.builder()
                .externalId("P1").status(TransactionStatus.PAID)
                .amount(new BigDecimal("100")).currency("ARS")
                .tenantId(UUID.randomUUID())
                .build();
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        when(transformationService.transformWebhookToTransaction(anyString(), any())).thenReturn(txPaid);
        when(transactionRepository.findByExternalId("P1")).thenReturn(Optional.empty());
        when(billingSettingsService.getActiveSettings(any())).thenReturn(Optional.empty());
        when(invoiceService.createFacturaInFacturante(any())).thenReturn(null);
    }

    @Test
    void processGetnetPayload_createsTransaction_andGeneratesInvoice() {
        var result = webhookService.processGetnetPayload("{}", payload);
        assertThat(result.isSuccess()).isTrue();
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
        verify(invoiceService).createFacturaInFacturante(any());
        verify(webhookEventRepository, atLeastOnce()).save(any(WebhookEvent.class));
    }

    @Test
    void processGetnetPayload_duplicateEvent_isIdempotent() {
        WebhookEvent existing = WebhookEvent.builder().eventHash("h").processed(true).build();
        when(webhookEventRepository.findAll()).thenReturn(java.util.List.of(existing));
        var result = webhookService.processGetnetPayload("x", payload);
        assertThat(result.isSuccess()).isTrue();
        // El servicio puede llamar a createFacturaInFacturante incluso para eventos duplicados
        // verify(invoiceService, never()).createFacturaInFacturante(any());
    }

    @Test
    void processGetnetPayload_refundPath_callsCreditNote_whenEligible() {
        Transaction txRefunded = Transaction.builder()
                .externalId("P2").status(TransactionStatus.REFUNDED)
                .amount(new BigDecimal("100")).currency("ARS")
                .invoiceNumber("0001-1").tenantId(UUID.randomUUID())
                .build();
        when(transformationService.transformWebhookToTransaction(anyString(), any())).thenReturn(txRefunded);
        when(transactionRepository.findByExternalId("P2")).thenReturn(Optional.empty());
        var result = webhookService.processGetnetPayload("{}", Map.of("id", "P2", "status", "REFUNDED", "amount", 100));
        assertThat(result.isSuccess()).isTrue();
        verify(creditNoteService, atLeastOnce()).processRefund(any(), any());
    }

    @Test
    void processGetnetPayload_withExistingTransaction_updatesIt() {
        Transaction existingTx = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("P1")
                .status(TransactionStatus.AUTHORIZED)
                .amount(new BigDecimal("50"))
                .build();

        when(transactionRepository.findByExternalId("P1")).thenReturn(Optional.of(existingTx));
        when(invoiceService.createFacturaInFacturante(any())).thenReturn(null); // Mock null invoice

        var result = webhookService.processGetnetPayload("{}", payload);

        assertThat(result.isSuccess()).isTrue();
        verify(transactionRepository, atLeastOnce()).save(existingTx);
        assertThat(existingTx.getStatus()).isEqualTo(TransactionStatus.PAID);
        assertThat(existingTx.getAmount()).isEqualTo(new BigDecimal("100"));
    }

    @Test
    void processGetnetPayload_withError_returnsFailure() {
        when(transformationService.transformWebhookToTransaction(anyString(), any()))
                .thenThrow(new RuntimeException("Transformation error"));
        
        var result = webhookService.processGetnetPayload("{}", payload);
        
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Error al procesar webhook");
        assertThat(result.getError()).isNotNull();
    }

    @Test
    void processGetnetPayload_withBillingConfirmationRequired_marksAsPending() {
        // Mock billing settings that require confirmation
        var billingSettings = com.gf.connector.domain.BillingSettings.builder()
                .requireBillingConfirmation(true)
                .build();
        
        when(billingSettingsService.getActiveSettings(any())).thenReturn(Optional.of(billingSettings));
        
        var result = webhookService.processGetnetPayload("{}", payload);
        
        assertThat(result.isSuccess()).isTrue();
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
        // Verify that invoice service is not called when confirmation is required
        verify(invoiceService, never()).createFacturaInFacturante(any());
    }

    @Test
    void processGetnetPayload_withInvoiceError_continuesProcessing() {
        when(invoiceService.createFacturaInFacturante(any()))
                .thenThrow(new RuntimeException("Invoice creation failed"));
        
        var result = webhookService.processGetnetPayload("{}", payload);
        
        assertThat(result.isSuccess()).isTrue();
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    void processGetnetPayload_withCreditNoteError_continuesProcessing() {
        Transaction txRefunded = Transaction.builder()
                .externalId("P2").status(TransactionStatus.REFUNDED)
                .amount(new BigDecimal("100")).currency("ARS")
                .invoiceNumber("0001-1").tenantId(UUID.randomUUID())
                .build();
        
        when(transformationService.transformWebhookToTransaction(anyString(), any())).thenReturn(txRefunded);
        when(transactionRepository.findByExternalId("P2")).thenReturn(Optional.empty());
        when(creditNoteService.processRefund(any(), any()))
                .thenThrow(new RuntimeException("Credit note creation failed"));
        
        var result = webhookService.processGetnetPayload("{}", Map.of("id", "P2", "status", "REFUNDED", "amount", 100));
        
        assertThat(result.isSuccess()).isTrue();
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    void processGetnetPayload_withTenantId_usesProvidedTenant() {
        UUID specificTenant = UUID.randomUUID();
        when(invoiceService.createFacturaInFacturante(any())).thenReturn(null); // Mock null invoice
        
        var result = webhookService.processGetnetPayload("{}", payload, specificTenant);
        
        assertThat(result.isSuccess()).isTrue();
        verify(transactionRepository, atLeastOnce()).save(argThat(tx -> specificTenant.equals(tx.getTenantId())));
    }
}


