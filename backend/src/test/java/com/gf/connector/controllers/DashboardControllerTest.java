package com.gf.connector.controllers;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para DashboardController
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private CreditNoteRepository creditNoteRepository;

    @InjectMocks
    private DashboardController dashboardController;

    private UUID testTenantId;
    private OffsetDateTime testStartDate;
    private OffsetDateTime testEndDate;
    private List<Transaction> testTransactions;

    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        testStartDate = LocalDate.now().minusDays(30).atStartOfDay().atOffset(ZoneOffset.UTC);
        testEndDate = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        
        testTransactions = new ArrayList<>();
        
        // Crear transacciones de prueba
        testTransactions.add(createTransaction(TransactionStatus.PAID, new BigDecimal("100.00"), "pending"));
        testTransactions.add(createTransaction(TransactionStatus.PAID, new BigDecimal("200.00"), "billed"));
        testTransactions.add(createTransaction(TransactionStatus.AUTHORIZED, new BigDecimal("150.00"), "pending"));
        testTransactions.add(createTransaction(TransactionStatus.FAILED, new BigDecimal("50.00"), "pending"));
        testTransactions.add(createTransaction(TransactionStatus.REFUNDED, new BigDecimal("100.00"), "billed"));
    }

    @Test
    void getDashboardStats_SinTenantId_Retorna401() {
        ResponseEntity<DashboardController.DashboardStatsDto> response = 
            dashboardController.getDashboardStats(null, null, null);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getDashboardStats_ConDatosValidos_RetornaEstadisticas() {
        // Arrange
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
            eq(testTenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(testTransactions);
        
        when(invoiceRepository.countByTenantIdAndCreatedAtBetween(
            eq(testTenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(3L);
        
        // WebhookEvent no tiene tenantId - es global
        when(webhookEventRepository.countByCreatedAtBetweenAndProcessedFalse(
            any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(2L);

        // Act
        ResponseEntity<DashboardController.DashboardStatsDto> response = 
            dashboardController.getDashboardStats(testTenantId, testStartDate.toLocalDate().toString(), 
                                                  testEndDate.toLocalDate().toString());

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        DashboardController.DashboardStatsDto stats = response.getBody();
        assertNotNull(stats);
        assertEquals(5L, stats.getTotalTransactions());
        assertEquals(3L, stats.getTotalInvoices());
        assertEquals(600.0, stats.getTotalAmount());
        assertEquals(2L, stats.getErrorCount());
        assertEquals(3L, stats.getPendingTransactions()); // 3 transacciones con billingStatus=pending
        
        // Tasa de éxito: (2 PAID + 1 AUTHORIZED) / 5 * 100 = 60%
        assertEquals(60.0, stats.getSuccessRate());
        
        assertEquals(2L, stats.getPaidTransactions());
        assertEquals(1L, stats.getAuthorizedTransactions());
        assertEquals(1L, stats.getRefundedTransactions());
        assertEquals(1L, stats.getFailedTransactions());
    }

    @Test
    void getTransactionsByDay_SinTenantId_Retorna401() {
        ResponseEntity<List<DashboardController.DailyStatsDto>> response = 
            dashboardController.getTransactionsByDay(null, null, null);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getTransactionsByDay_ConDatos_RetornaEstadisticasDiarias() {
        // Arrange
        OffsetDateTime today = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime yesterday = today.minusDays(1);
        
        List<Transaction> transactions = Arrays.asList(
            createTransactionWithDate(TransactionStatus.PAID, new BigDecimal("100.00"), today),
            createTransactionWithDate(TransactionStatus.PAID, new BigDecimal("200.00"), today),
            createTransactionWithDate(TransactionStatus.PAID, new BigDecimal("150.00"), yesterday)
        );
        
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
            eq(testTenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(transactions);

        // Act
        ResponseEntity<List<DashboardController.DailyStatsDto>> response = 
            dashboardController.getTransactionsByDay(testTenantId, testStartDate.toLocalDate().toString(), 
                                                     testEndDate.toLocalDate().toString());

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        List<DashboardController.DailyStatsDto> dailyStats = response.getBody();
        assertNotNull(dailyStats);
        assertEquals(2, dailyStats.size()); // 2 días diferentes
    }

    @Test
    void getInvoicesByStatus_SinTenantId_Retorna401() {
        ResponseEntity<Map<String, Long>> response = 
            dashboardController.getInvoicesByStatus(null);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getReconciliationSummary_SinTenantId_Retorna401() {
        ResponseEntity<DashboardController.ReconciliationSummaryDto> response = 
            dashboardController.getReconciliationSummary(null);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getReconciliationSummary_ConDatos_RetornaResumen() {
        // Arrange
        Transaction reconciledTx = createTransaction(TransactionStatus.PAID, new BigDecimal("100.00"), "billed");
        reconciledTx.setReconciled(true);
        
        Transaction unreconciledTx = createTransaction(TransactionStatus.PAID, new BigDecimal("100.00"), "billed");
        unreconciledTx.setReconciled(false);
        
        List<Transaction> transactions = Arrays.asList(reconciledTx, unreconciledTx);
        
        when(transactionRepository.findAll()).thenReturn(transactions);

        // Act
        ResponseEntity<DashboardController.ReconciliationSummaryDto> response = 
            dashboardController.getReconciliationSummary(testTenantId);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        DashboardController.ReconciliationSummaryDto summary = response.getBody();
        assertNotNull(summary);
        assertEquals(2L, summary.getTotalTransactions());
        assertEquals(1L, summary.getReconciledTransactions());
        assertEquals(1L, summary.getUnreconciledTransactions());
        assertEquals(50.0, summary.getReconciliationRate());
    }

    @Test
    void getCreditNotesStats_SinTenantId_Retorna401() {
        ResponseEntity<DashboardController.CreditNoteStatsDto> response = 
            dashboardController.getCreditNotesStats(null);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getCreditNotesStats_ConDatos_RetornaEstadisticas() {
        // Arrange
        when(creditNoteRepository.findAll()).thenReturn(new ArrayList<>());
        when(creditNoteRepository.countByStatus("pending")).thenReturn(5L);
        when(creditNoteRepository.countByStatus("sent")).thenReturn(10L);
        when(creditNoteRepository.countByStatus("error")).thenReturn(2L);
        when(creditNoteRepository.countByStatus("stub")).thenReturn(3L);
        when(creditNoteRepository.countByStrategy("automatic")).thenReturn(12L);
        when(creditNoteRepository.countByStrategy("manual")).thenReturn(8L);

        // Act
        ResponseEntity<DashboardController.CreditNoteStatsDto> response = 
            dashboardController.getCreditNotesStats(testTenantId);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        DashboardController.CreditNoteStatsDto stats = response.getBody();
        assertNotNull(stats);
        assertEquals(5L, stats.getPendingCreditNotes());
        assertEquals(10L, stats.getSentCreditNotes());
        assertEquals(2L, stats.getErrorCreditNotes());
        assertEquals(3L, stats.getStubCreditNotes());
        assertEquals(12L, stats.getAutomaticCreditNotes());
        assertEquals(8L, stats.getManualCreditNotes());
    }

    // ===== Métodos auxiliares =====

    private Transaction createTransaction(TransactionStatus status, BigDecimal amount, String billingStatus) {
        Transaction tx = Transaction.builder()
            .id(UUID.randomUUID())
            .externalId("TXN_" + UUID.randomUUID().toString())
            .amount(amount)
            .currency("ARS")
            .status(status)
            .billingStatus(billingStatus)
            .tenantId(testTenantId)
            .reconciled(false)
            .build();
        
        tx.prePersist();
        return tx;
    }

    private Transaction createTransactionWithDate(TransactionStatus status, BigDecimal amount, OffsetDateTime date) {
        Transaction tx = createTransaction(status, amount, "pending");
        // Simulamos la fecha de creación
        tx.setCreatedAt(date);
        return tx;
    }
}

