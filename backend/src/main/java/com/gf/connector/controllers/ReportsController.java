package com.gf.connector.controllers;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.Invoice;
import com.gf.connector.domain.CreditNote;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador para generación de reportes y exportación de datos
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

    private static final Logger log = LoggerFactory.getLogger(ReportsController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final WebhookEventRepository webhookEventRepository;

    /**
     * Exporta transacciones a CSV
     * Endpoint: GET /api/reports/transactions/export?format=csv&startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping("/transactions/export")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId,
            @RequestParam(required = false, defaultValue = "csv") String format,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status) {
        
        if (tenantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            log.info("Exportando transacciones en formato {} para tenant {}", format, tenantId);

            // Parsear fechas
            OffsetDateTime start = parseDate(startDate, LocalDate.now().minusMonths(1));
            OffsetDateTime end = parseDate(endDate, LocalDate.now());

            // Obtener transacciones
            List<Transaction> transactions = transactionRepository
                .findByTenantIdAndCreatedAtBetween(tenantId, start, end);

            // Filtrar por estado si se especifica
            if (status != null && !status.isEmpty()) {
                try {
                    TransactionStatus statusEnum = TransactionStatus.valueOf(status.toUpperCase());
                    transactions = transactions.stream()
                        .filter(t -> t.getStatus() == statusEnum)
                        .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    log.warn("Estado inválido: {}", status);
                }
            }

            // Generar archivo según formato
            byte[] fileContent;
            String fileName;
            String contentType;

            if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
                // Por ahora, exportamos como CSV también para Excel (puede abrirse en Excel)
                fileContent = generateTransactionsCsv(transactions);
                fileName = "transacciones_" + LocalDate.now() + ".csv";
                contentType = "text/csv";
            } else {
                // CSV por defecto
                fileContent = generateTransactionsCsv(transactions);
                fileName = "transacciones_" + LocalDate.now() + ".csv";
                contentType = "text/csv";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            log.info("Exportadas {} transacciones", transactions.size());

            return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);

        } catch (Exception e) {
            log.error("Error exportando transacciones", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Exporta facturas a CSV
     */
    @GetMapping("/invoices/export")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<byte[]> exportInvoices(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId,
            @RequestParam(required = false, defaultValue = "csv") String format,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        if (tenantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            log.info("Exportando facturas para tenant {}", tenantId);

            OffsetDateTime start = parseDate(startDate, LocalDate.now().minusMonths(1));
            OffsetDateTime end = parseDate(endDate, LocalDate.now());

            // Obtener facturas del período
            List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(inv -> tenantId.equals(inv.getTenantId()))
                .filter(inv -> inv.getCreatedAt().isAfter(start) && inv.getCreatedAt().isBefore(end))
                .collect(Collectors.toList());

            byte[] fileContent = generateInvoicesCsv(invoices);
            String fileName = "facturas_" + LocalDate.now() + ".csv";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", fileName);

            log.info("Exportadas {} facturas", invoices.size());

            return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);

        } catch (Exception e) {
            log.error("Error exportando facturas", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Exporta notas de crédito a CSV
     */
    @GetMapping("/credit-notes/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportCreditNotes(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId,
            @RequestParam(required = false, defaultValue = "csv") String format) {
        
        if (tenantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            log.info("Exportando notas de crédito para tenant {}", tenantId);

            List<CreditNote> creditNotes = creditNoteRepository.findAll();

            byte[] fileContent = generateCreditNotesCsv(creditNotes);
            String fileName = "notas_credito_" + LocalDate.now() + ".csv";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", fileName);

            log.info("Exportadas {} notas de crédito", creditNotes.size());

            return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);

        } catch (Exception e) {
            log.error("Error exportando notas de crédito", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Genera reporte de reconciliación
     */
    @GetMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ReconciliationReportDto> getReconciliationReport(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        if (tenantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            OffsetDateTime start = parseDate(startDate, LocalDate.now().minusMonths(1));
            OffsetDateTime end = parseDate(endDate, LocalDate.now());

            List<Transaction> transactions = transactionRepository
                .findByTenantIdAndCreatedAtBetween(tenantId, start, end);

            // Calcular estadísticas de reconciliación
            long totalTransactions = transactions.size();
            long reconciledCount = transactions.stream().filter(Transaction::isReconciled).count();
            long unreconciledCount = totalTransactions - reconciledCount;

            // Transacciones huérfanas (no facturadas pero pagadas)
            List<TransactionSummaryDto> orphanTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PAID)
                .filter(t -> "pending".equals(t.getBillingStatus()))
                .map(this::toTransactionSummary)
                .collect(Collectors.toList());

            // Transacciones con errores
            List<TransactionSummaryDto> errorTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                .map(this::toTransactionSummary)
                .collect(Collectors.toList());

            double reconciliationRate = totalTransactions > 0 
                ? ((double) reconciledCount / totalTransactions) * 100.0
                : 0.0;

            ReconciliationReportDto report = ReconciliationReportDto.builder()
                .startDate(start.toLocalDate().toString())
                .endDate(end.toLocalDate().toString())
                .totalTransactions(totalTransactions)
                .reconciledTransactions(reconciledCount)
                .unreconciledTransactions(unreconciledCount)
                .reconciliationRate(reconciliationRate)
                .orphanTransactions(orphanTransactions)
                .errorTransactions(errorTransactions)
                .build();

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("Error generando reporte de reconciliación", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Genera reporte consolidado con todas las métricas
     */
    @GetMapping("/consolidated")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ConsolidatedReportDto> getConsolidatedReport(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        if (tenantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            OffsetDateTime start = parseDate(startDate, LocalDate.now().minusMonths(1));
            OffsetDateTime end = parseDate(endDate, LocalDate.now());

            // Obtener todas las entidades
            List<Transaction> transactions = transactionRepository
                .findByTenantIdAndCreatedAtBetween(tenantId, start, end);
            
            long invoiceCount = invoiceRepository.countByTenantIdAndCreatedAtBetween(tenantId, start, end);
            // Nota: Los webhooks no tienen tenantId, son globales
            long webhookErrors = webhookEventRepository
                .countByCreatedAtBetweenAndProcessedFalse(start, end);

            // Calcular totales por estado
            Map<String, Long> transactionsByStatus = transactions.stream()
                .collect(Collectors.groupingBy(
                    t -> t.getStatus().name(),
                    Collectors.counting()
                ));

            // Calcular montos
            double totalAmount = transactions.stream()
                .filter(t -> t.getAmount() != null)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();

            double paidAmount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PAID)
                .filter(t -> t.getAmount() != null)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();

            double refundedAmount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.REFUNDED)
                .filter(t -> t.getAmount() != null)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();

            ConsolidatedReportDto report = ConsolidatedReportDto.builder()
                .periodStart(start.toLocalDate().toString())
                .periodEnd(end.toLocalDate().toString())
                .totalTransactions(Long.valueOf(transactions.size()))
                .totalInvoices(invoiceCount)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .refundedAmount(refundedAmount)
                .netAmount(paidAmount - refundedAmount)
                .transactionsByStatus(transactionsByStatus)
                .webhookErrors(webhookErrors)
                .generatedAt(OffsetDateTime.now().format(DATE_FORMATTER))
                .build();

            return ResponseEntity.ok(report);

        } catch (Exception e) {
            log.error("Error generando reporte consolidado", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===== MÉTODOS PRIVADOS DE GENERACIÓN CSV =====

    private byte[] generateTransactionsCsv(List<Transaction> transactions) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos)) {

            // Header
            writer.println("ID,ID Externo,Monto,Moneda,Estado,Estado Facturación,CUIT,Número Factura,CAE,Fecha Creación,Fecha Actualización");

            // Datos
            for (Transaction t : transactions) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    nvl(t.getId()),
                    nvl(t.getExternalId()),
                    nvl(t.getAmount()),
                    nvl(t.getCurrency()),
                    nvl(t.getStatus()),
                    nvl(t.getBillingStatus()),
                    nvl(t.getCustomerDoc()),
                    nvl(t.getInvoiceNumber()),
                    nvl(t.getCae()),
                    nvl(t.getCreatedAt()),
                    nvl(t.getUpdatedAt())
                ));
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando CSV de transacciones", e);
            throw new RuntimeException("Error generando CSV", e);
        }
    }

    private byte[] generateInvoicesCsv(List<Invoice> invoices) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos)) {

            // Header
            writer.println("ID,Transaction ID,Estado,PDF URL,Fecha Creación");

            // Datos
            for (Invoice inv : invoices) {
                writer.println(String.format("%s,%s,%s,%s,%s",
                    nvl(inv.getId()),
                    inv.getTransaction() != null ? nvl(inv.getTransaction().getId()) : "",
                    nvl(inv.getStatus()),
                    nvl(inv.getPdfUrl()),
                    nvl(inv.getCreatedAt())
                ));
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando CSV de facturas", e);
            throw new RuntimeException("Error generando CSV", e);
        }
    }

    private byte[] generateCreditNotesCsv(List<CreditNote> creditNotes) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             PrintWriter writer = new PrintWriter(baos)) {

            // Header
            writer.println("ID,Número NC,CAE,Estado,Estrategia,Motivo,Transaction ID,Fecha Creación");

            // Datos
            for (CreditNote cn : creditNotes) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                    nvl(cn.getId()),
                    nvl(cn.getCreditNoteNumber()),
                    nvl(cn.getCreditNoteCae()),
                    nvl(cn.getStatus()),
                    nvl(cn.getStrategy()),
                    nvl(cn.getRefundReason()),
                    cn.getTransaction() != null ? nvl(cn.getTransaction().getId()) : "",
                    nvl(cn.getCreatedAt())
                ));
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generando CSV de notas de crédito", e);
            throw new RuntimeException("Error generando CSV", e);
        }
    }

    private String nvl(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private OffsetDateTime parseDate(String dateStr, LocalDate defaultDate) {
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                return LocalDate.parse(dateStr).atStartOfDay().atOffset(ZoneOffset.UTC);
            } catch (Exception e) {
                log.warn("Error parseando fecha '{}', usando default", dateStr);
            }
        }
        return defaultDate.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private TransactionSummaryDto toTransactionSummary(Transaction t) {
        return TransactionSummaryDto.builder()
            .id(t.getId().toString())
            .externalId(t.getExternalId())
            .amount(t.getAmount() != null ? t.getAmount().doubleValue() : 0.0)
            .status(t.getStatus().name())
            .billingStatus(t.getBillingStatus())
            .createdAt(t.getCreatedAt().format(DATE_FORMATTER))
            .build();
    }

    // ===== DTOs =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationReportDto {
        private String startDate;
        private String endDate;
        private Long totalTransactions;
        private Long reconciledTransactions;
        private Long unreconciledTransactions;
        private Double reconciliationRate;
        private List<TransactionSummaryDto> orphanTransactions;
        private List<TransactionSummaryDto> errorTransactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionSummaryDto {
        private String id;
        private String externalId;
        private Double amount;
        private String status;
        private String billingStatus;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsolidatedReportDto {
        private String periodStart;
        private String periodEnd;
        private Long totalTransactions;
        private Long totalInvoices;
        private Double totalAmount;
        private Double paidAmount;
        private Double refundedAmount;
        private Double netAmount;
        private Map<String, Long> transactionsByStatus;
        private Long webhookErrors;
        private String generatedAt;
    }
}

