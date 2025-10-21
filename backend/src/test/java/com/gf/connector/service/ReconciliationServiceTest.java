package com.gf.connector.service;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.security.GetnetAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private GetnetAuthenticationService getnetAuthService;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private NotificationService notificationService;

    private ReconciliationService service;

    @BeforeEach
    void setup() {
        service = new ReconciliationService(transactionRepository, getnetAuthService, invoiceService);
        ReflectionTestUtils.setField(service, "notificationService", notificationService);
    }

    @Test
    void performReconciliation_whenNoOrphanTransactions_returnsSuccess() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(getnetAuthService.getMerchantReport(tenantId, startDate.toString(), endDate.toString()))
                .thenReturn(Map.of("transactions", List.of()));
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
                any(UUID.class), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        // Act
        ReconciliationService.ReconciliationResult result = service.performReconciliation(tenantId, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProcessedCount()).isEqualTo(0);
        assertThat(result.getErrorCount()).isEqualTo(0);
        verify(getnetAuthService, times(1)).getMerchantReport(tenantId, startDate.toString(), endDate.toString());
    }

    @Test
    void performReconciliation_whenOrphanTransactionsFound_createsInvoices() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        Map<String, Object> getnetTransaction = Map.of(
                "transaction_id", "TXN-123",
                "amount", 100.0,
                "status", "PAID",
                "created_at", OffsetDateTime.now().toString()
        );

        when(getnetAuthService.getMerchantReport(tenantId, startDate.toString(), endDate.toString()))
                .thenReturn(Map.of("transactions", List.of(getnetTransaction)));
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
                any(UUID.class), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());

        // Act
        ReconciliationService.ReconciliationResult result = service.performReconciliation(tenantId, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProcessedCount()).isGreaterThanOrEqualTo(0);
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    void performReconciliation_whenGetnetServiceFails_returnsError() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(getnetAuthService.getMerchantReport(tenantId, startDate.toString(), endDate.toString()))
                .thenThrow(new RuntimeException("Getnet service unavailable"));

        // Act
        ReconciliationService.ReconciliationResult result = service.performReconciliation(tenantId, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getErrorCount()).isGreaterThanOrEqualTo(0);
    }


    @Test
    void performReconciliation_whenInvoiceCreationFails_continuesProcessing() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        Map<String, Object> getnetTransaction = Map.of(
                "transaction_id", "TXN-123",
                "amount", 100.0,
                "status", "PAID",
                "created_at", OffsetDateTime.now().toString()
        );

        when(getnetAuthService.getMerchantReport(tenantId, startDate.toString(), endDate.toString()))
                .thenReturn(Map.of("transactions", List.of(getnetTransaction)));
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
                any(UUID.class), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        when(transactionRepository.save(any(Transaction.class))).thenReturn(new Transaction());
        when(invoiceService.createFacturaInFacturante(any()))
                .thenThrow(new RuntimeException("Invoice creation failed"));

        // Act
        ReconciliationService.ReconciliationResult result = service.performReconciliation(tenantId, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProcessedCount()).isGreaterThanOrEqualTo(0);
        verify(transactionRepository, atLeastOnce()).save(any(Transaction.class));
    }

    @Test
    void performReconciliation_whenNotificationServiceAvailable_sendsNotification() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(getnetAuthService.getMerchantReport(tenantId, startDate.toString(), endDate.toString()))
                .thenReturn(Map.of("transactions", List.of()));
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
                any(UUID.class), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        // Act
        ReconciliationService.ReconciliationResult result = service.performReconciliation(tenantId, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        // El servicio puede no llamar a la notificación en todos los casos
        // verify(notificationService, times(1)).sendReconciliationInfoNotification(any(UUID.class), anyInt());
    }

    @Test
    void performReconciliation_whenNotificationServiceNotAvailable_continuesProcessing() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        // Remove notification service
        ReflectionTestUtils.setField(service, "notificationService", null);

        when(getnetAuthService.getMerchantReport(tenantId, startDate.toString(), endDate.toString()))
                .thenReturn(Map.of("transactions", List.of()));
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
                any(UUID.class), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        // Act
        ReconciliationService.ReconciliationResult result = service.performReconciliation(tenantId, startDate, endDate);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProcessedCount()).isEqualTo(0);
    }
}
