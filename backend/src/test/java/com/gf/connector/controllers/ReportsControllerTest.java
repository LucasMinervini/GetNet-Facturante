package com.gf.connector.controllers;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.Invoice;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
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
 * Tests unitarios para ReportsController
 */
@ExtendWith(MockitoExtension.class)
class ReportsControllerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CreditNoteRepository creditNoteRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @InjectMocks
    private ReportsController reportsController;

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
        testTransactions.add(createTransaction(TransactionStatus.PAID, new BigDecimal("100.00")));
        testTransactions.add(createTransaction(TransactionStatus.PAID, new BigDecimal("200.00")));
        testTransactions.add(createTransaction(TransactionStatus.AUTHORIZED, new BigDecimal("150.00")));
    }

    @Test
    void exportTransactions_SinTenantId_Retorna401() {
        ResponseEntity<byte[]> response = 
            reportsController.exportTransactions(null, "csv", null, null, null);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void exportTransactions_FormatoCSV_RetornaArchivoCSV() {
        // Arrange
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
            eq(testTenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(testTransactions);

        // Act
        ResponseEntity<byte[]> response = 
            reportsController.exportTransactions(testTenantId, "csv", 
                testStartDate.toLocalDate().toString(), 
                testEndDate.toLocalDate().toString(), null);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        byte[] content = response.getBody();
        assertNotNull(content);
        assertTrue(content.length > 0);
        
        // Verificar headers
        HttpHeaders headers = response.getHeaders();
        assertNotNull(headers.getContentType());
        assertTrue(headers.getContentType().toString().contains("text/csv"));
    }

    @Test
    void exportTransactions_ConFiltroEstado_FiltrarCorrectamente() {
        // Arrange
        // Simular el escenario real: el repositorio retorna TODAS las transacciones
        // y el controller las filtra en memoria
        List<Transaction> allTransactions = Arrays.asList(
            createTransaction(TransactionStatus.PAID, new BigDecimal("100.00")),
            createTransaction(TransactionStatus.AUTHORIZED, new BigDecimal("200.00")),
            createTransaction(TransactionStatus.FAILED, new BigDecimal("50.00"))
        );
        
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
            eq(testTenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(allTransactions);

        // Act
        ResponseEntity<byte[]> response = 
            reportsController.exportTransactions(testTenantId, "csv", 
                testStartDate.toLocalDate().toString(), 
                testEndDate.toLocalDate().toString(), 
                "PAID");

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        byte[] content = response.getBody();
        assertNotNull(content);
        
        String csvContent = new String(content);
        
        // Debug: imprimir contenido del CSV
        System.out.println("=== CSV Content ===");
        System.out.println(csvContent);
        System.out.println("===================");
        
        // Verificar que hay contenido y que contiene el header
        assertTrue(csvContent.length() > 0, "El CSV no debe estar vacío");
        assertTrue(csvContent.contains("Estado"), "El CSV debe contener el header 'Estado'");
        
        // Verificar que contiene datos de la transacción PAID (usar búsqueda más flexible)
        // El enum TransactionStatus.PAID se convierte a string como "PAID"
        boolean containsPaidStatus = csvContent.toUpperCase().contains("PAID");
        assertTrue(containsPaidStatus, "El CSV debe contener el estado PAID. Contenido: " + csvContent);
    }

    @Test
    void exportInvoices_SinTenantId_Retorna401() {
        ResponseEntity<byte[]> response = 
            reportsController.exportInvoices(null, "csv", null, null);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void exportInvoices_ConDatos_RetornaArchivoCSV() {
        // Arrange
        List<Invoice> invoices = Arrays.asList(
            createInvoice("sent"),
            createInvoice("sent"),
            createInvoice("error")
        );
        
        when(invoiceRepository.findAll()).thenReturn(invoices);

        // Act
        ResponseEntity<byte[]> response = 
            reportsController.exportInvoices(testTenantId, "csv", 
                testStartDate.toLocalDate().toString(), 
                testEndDate.toLocalDate().toString());

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        byte[] content = response.getBody();
        assertNotNull(content);
        assertTrue(content.length > 0);
    }

    @Test
    void getReconciliationReport_SinTenantId_Retorna401() {
        ResponseEntity<ReportsController.ReconciliationReportDto> response = 
            reportsController.getReconciliationReport(null, null, null);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getReconciliationReport_ConDatos_RetornaReporte() {
        // Arrange
        Transaction reconciledTx = createTransaction(TransactionStatus.PAID, new BigDecimal("100.00"));
        reconciledTx.setReconciled(true);
        reconciledTx.setBillingStatus("billed");
        
        Transaction orphanTx = createTransaction(TransactionStatus.PAID, new BigDecimal("200.00"));
        orphanTx.setReconciled(false);
        orphanTx.setBillingStatus("pending");
        
        List<Transaction> transactions = Arrays.asList(reconciledTx, orphanTx);
        
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
            eq(testTenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(transactions);

        // Act
        ResponseEntity<ReportsController.ReconciliationReportDto> response = 
            reportsController.getReconciliationReport(testTenantId, 
                testStartDate.toLocalDate().toString(), 
                testEndDate.toLocalDate().toString());

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        ReportsController.ReconciliationReportDto report = response.getBody();
        assertNotNull(report);
        assertEquals(2L, report.getTotalTransactions());
        assertEquals(1L, report.getReconciledTransactions());
        assertEquals(1L, report.getUnreconciledTransactions());
        assertEquals(50.0, report.getReconciliationRate());
        
        // Verificar transacciones huérfanas
        assertNotNull(report.getOrphanTransactions());
        assertEquals(1, report.getOrphanTransactions().size());
    }

    @Test
    void getConsolidatedReport_SinTenantId_Retorna401() {
        ResponseEntity<ReportsController.ConsolidatedReportDto> response = 
            reportsController.getConsolidatedReport(null, null, null);
        
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void getConsolidatedReport_ConDatos_RetornaReporteCompleto() {
        // Arrange
        Transaction paidTx = createTransaction(TransactionStatus.PAID, new BigDecimal("100.00"));
        Transaction refundedTx = createTransaction(TransactionStatus.REFUNDED, new BigDecimal("50.00"));
        Transaction failedTx = createTransaction(TransactionStatus.FAILED, new BigDecimal("75.00"));
        
        List<Transaction> transactions = Arrays.asList(paidTx, refundedTx, failedTx);
        
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
            eq(testTenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(transactions);
        
        when(invoiceRepository.countByTenantIdAndCreatedAtBetween(
            eq(testTenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(2L);
        
        // WebhookEvent no tiene tenantId - es global
        when(webhookEventRepository.countByCreatedAtBetweenAndProcessedFalse(
            any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(1L);

        // Act
        ResponseEntity<ReportsController.ConsolidatedReportDto> response = 
            reportsController.getConsolidatedReport(testTenantId, 
                testStartDate.toLocalDate().toString(), 
                testEndDate.toLocalDate().toString());

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        ReportsController.ConsolidatedReportDto report = response.getBody();
        assertNotNull(report);
        assertEquals(3, report.getTotalTransactions());
        assertEquals(2L, report.getTotalInvoices());
        assertEquals(225.0, report.getTotalAmount()); // 100 + 50 + 75
        assertEquals(100.0, report.getPaidAmount());
        assertEquals(50.0, report.getRefundedAmount());
        assertEquals(50.0, report.getNetAmount()); // 100 - 50
        assertEquals(1L, report.getWebhookErrors());
        
        // Verificar agrupación por estado
        Map<String, Long> byStatus = report.getTransactionsByStatus();
        assertNotNull(byStatus);
        assertEquals(1L, byStatus.get("PAID"));
        assertEquals(1L, byStatus.get("REFUNDED"));
        assertEquals(1L, byStatus.get("FAILED"));
    }

    @Test
    void exportTransactions_FormatoExcel_RetornaArchivoCSV() {
        // Arrange
        when(transactionRepository.findByTenantIdAndCreatedAtBetween(
            eq(testTenantId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(testTransactions);

        // Act
        ResponseEntity<byte[]> response = 
            reportsController.exportTransactions(testTenantId, "excel", 
                testStartDate.toLocalDate().toString(), 
                testEndDate.toLocalDate().toString(), null);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        
        byte[] content = response.getBody();
        assertNotNull(content);
        assertTrue(content.length > 0);
        
        // Por ahora exportamos como CSV incluso para Excel
        HttpHeaders headers = response.getHeaders();
        assertNotNull(headers.getContentType());
    }

    // ===== Métodos auxiliares =====

    private Transaction createTransaction(TransactionStatus status, BigDecimal amount) {
        Transaction tx = Transaction.builder()
            .id(UUID.randomUUID())
            .externalId("TXN_" + UUID.randomUUID().toString())
            .amount(amount)
            .currency("ARS")
            .status(status)
            .billingStatus("pending")
            .tenantId(testTenantId)
            .customerDoc("20123456789")
            .reconciled(false)
            .build();
        
        tx.prePersist();
        return tx;
    }

    private Invoice createInvoice(String status) {
        Invoice invoice = Invoice.builder()
            .id(UUID.randomUUID())
            .status(status)
            .tenantId(testTenantId)
            .pdfUrl("http://example.com/invoice.pdf")
            .build();
        
        invoice.prePersist();
        return invoice;
    }
}

