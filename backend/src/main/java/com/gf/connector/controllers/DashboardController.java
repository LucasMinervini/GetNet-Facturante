package com.gf.connector.controllers;

import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Controlador para Dashboard con estadísticas y métricas
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final TransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final CreditNoteRepository creditNoteRepository;

    /**
     * Obtiene estadísticas generales del dashboard
     * Endpoint: GET /api/dashboard/stats?startDate=2024-01-01&endDate=2024-12-31
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<DashboardStatsDto> getDashboardStats(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        if (tenantId == null) {
            log.warn("Intento de acceso sin tenantId");
            return ResponseEntity.status(401).build();
        }

        try {
            // Parsear fechas o usar últimos 30 días por defecto
            OffsetDateTime start = parseDate(startDate, LocalDate.now().minusDays(30));
            OffsetDateTime end = parseDate(endDate, LocalDate.now());

            log.info("Cargando estadísticas de dashboard para tenant {} desde {} hasta {}", 
                     tenantId, start, end);

            // Obtener todas las transacciones del período
            List<com.gf.connector.domain.Transaction> transactions = 
                transactionRepository.findByTenantIdAndCreatedAtBetween(tenantId, start, end);

            // Calcular estadísticas
            long totalTransactions = transactions.size();
            
            // Contar por estado
            long paidCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PAID)
                .count();
            
            long authorizedCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.AUTHORIZED)
                .count();
            
            long refundedCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.REFUNDED)
                .count();
            
            long failedCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.FAILED)
                .count();

            // Calcular monto total
            double totalAmount = transactions.stream()
                .filter(t -> t.getAmount() != null)
                .mapToDouble(t -> t.getAmount().doubleValue())
                .sum();

            // Contar facturas emitidas
            long totalInvoices = invoiceRepository.countByTenantIdAndCreatedAtBetween(tenantId, start, end);

            // Contar transacciones pendientes de facturación
            long pendingTransactions = transactions.stream()
                .filter(t -> "pending".equals(t.getBillingStatus()))
                .count();

            // Contar errores (webhooks no procesados)
            // Nota: Los webhooks no tienen tenantId, son globales
            long errorCount = webhookEventRepository.countByCreatedAtBetweenAndProcessedFalse(start, end);

            // Calcular tasa de éxito (paid + authorized) / total * 100
            double successRate = totalTransactions > 0 
                ? ((double) (paidCount + authorizedCount) / totalTransactions) * 100.0
                : 0.0;

            DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalTransactions(totalTransactions)
                .totalInvoices(totalInvoices)
                .totalAmount(totalAmount)
                .errorCount(errorCount)
                .pendingTransactions(pendingTransactions)
                .successRate(successRate)
                .paidTransactions(paidCount)
                .authorizedTransactions(authorizedCount)
                .refundedTransactions(refundedCount)
                .failedTransactions(failedCount)
                .build();

            log.info("Estadísticas cargadas: {} transacciones, {} facturas", 
                     totalTransactions, totalInvoices);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error cargando estadísticas del dashboard", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene estadísticas de transacciones agrupadas por día
     * Útil para gráficos de volumen
     */
    @GetMapping("/transactions-by-day")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<DailyStatsDto>> getTransactionsByDay(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        if (tenantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            OffsetDateTime start = parseDate(startDate, LocalDate.now().minusDays(30));
            OffsetDateTime end = parseDate(endDate, LocalDate.now());

            List<com.gf.connector.domain.Transaction> transactions = 
                transactionRepository.findByTenantIdAndCreatedAtBetween(tenantId, start, end);

            // Agrupar por día
            Map<LocalDate, DailyStatsDto> dailyStatsMap = new LinkedHashMap<>();

            for (com.gf.connector.domain.Transaction transaction : transactions) {
                LocalDate date = transaction.getCreatedAt().toLocalDate();
                
                DailyStatsDto dailyStats = dailyStatsMap.computeIfAbsent(date, d -> 
                    DailyStatsDto.builder()
                        .date(d.toString())
                        .count(0L)
                        .amount(0.0)
                        .build()
                );

                dailyStats.setCount(dailyStats.getCount() + 1);
                if (transaction.getAmount() != null) {
                    dailyStats.setAmount(dailyStats.getAmount() + transaction.getAmount().doubleValue());
                }
            }

            List<DailyStatsDto> result = new ArrayList<>(dailyStatsMap.values());
            result.sort(Comparator.comparing(DailyStatsDto::getDate));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error cargando estadísticas diarias", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene estadísticas de facturas agrupadas por estado
     */
    @GetMapping("/invoices-by-status")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Map<String, Long>> getInvoicesByStatus(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId) {
        
        if (tenantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<com.gf.connector.domain.Invoice> invoices = invoiceRepository.findAll();
            
            Map<String, Long> statusCounts = new HashMap<>();
            invoices.stream()
                .filter(inv -> tenantId.equals(inv.getTenantId()))
                .forEach(inv -> {
                    String status = inv.getStatus() != null ? inv.getStatus() : "unknown";
                    statusCounts.put(status, statusCounts.getOrDefault(status, 0L) + 1);
                });

            return ResponseEntity.ok(statusCounts);

        } catch (Exception e) {
            log.error("Error cargando estadísticas de facturas", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene resumen de reconciliación
     */
    @GetMapping("/reconciliation-summary")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ReconciliationSummaryDto> getReconciliationSummary(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId) {
        
        if (tenantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<com.gf.connector.domain.Transaction> allTransactions = 
                transactionRepository.findAll();

            long reconciledCount = allTransactions.stream()
                .filter(t -> tenantId.equals(t.getTenantId()))
                .filter(com.gf.connector.domain.Transaction::isReconciled)
                .count();

            long unreconciledCount = allTransactions.stream()
                .filter(t -> tenantId.equals(t.getTenantId()))
                .filter(t -> !t.isReconciled())
                .count();

            long totalCount = reconciledCount + unreconciledCount;
            double reconciliationRate = totalCount > 0 
                ? ((double) reconciledCount / totalCount) * 100.0
                : 0.0;

            ReconciliationSummaryDto summary = ReconciliationSummaryDto.builder()
                .totalTransactions(totalCount)
                .reconciledTransactions(reconciledCount)
                .unreconciledTransactions(unreconciledCount)
                .reconciliationRate(reconciliationRate)
                .build();

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error cargando resumen de reconciliación", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtiene estadísticas de notas de crédito
     */
    @GetMapping("/credit-notes-stats")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<CreditNoteStatsDto> getCreditNotesStats(
            @RequestAttribute(name = "tenantId", required = false) UUID tenantId) {
        
        if (tenantId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<com.gf.connector.domain.CreditNote> allCreditNotes = creditNoteRepository.findAll();

            long totalCount = allCreditNotes.size();
            long pendingCount = creditNoteRepository.countByStatus("pending");
            long sentCount = creditNoteRepository.countByStatus("sent");
            long errorCount = creditNoteRepository.countByStatus("error");
            long stubCount = creditNoteRepository.countByStatus("stub");

            long automaticCount = creditNoteRepository.countByStrategy("automatic");
            long manualCount = creditNoteRepository.countByStrategy("manual");

            CreditNoteStatsDto stats = CreditNoteStatsDto.builder()
                .totalCreditNotes(totalCount)
                .pendingCreditNotes(pendingCount)
                .sentCreditNotes(sentCount)
                .errorCreditNotes(errorCount)
                .stubCreditNotes(stubCount)
                .automaticCreditNotes(automaticCount)
                .manualCreditNotes(manualCount)
                .build();

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error cargando estadísticas de notas de crédito", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper para parsear fechas desde String
     */
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

    // ===== DTOs =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStatsDto {
        private Long totalTransactions;
        private Long totalInvoices;
        private Double totalAmount;
        private Long errorCount;
        private Long pendingTransactions;
        private Double successRate;
        private Long paidTransactions;
        private Long authorizedTransactions;
        private Long refundedTransactions;
        private Long failedTransactions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStatsDto {
        private String date;
        private Long count;
        private Double amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationSummaryDto {
        private Long totalTransactions;
        private Long reconciledTransactions;
        private Long unreconciledTransactions;
        private Double reconciliationRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditNoteStatsDto {
        private Long totalCreditNotes;
        private Long pendingCreditNotes;
        private Long sentCreditNotes;
        private Long errorCreditNotes;
        private Long stubCreditNotes;
        private Long automaticCreditNotes;
        private Long manualCreditNotes;
    }
}

