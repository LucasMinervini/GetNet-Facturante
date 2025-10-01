package com.gf.connector.service;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final BillingSettingsService billingSettingsService;
    private final InvoiceService invoiceService;

    // T-1: cada hora revalida el último día (cron configurable a futuro)
    @Scheduled(cron = "0 15 * * * *")
    public void reconcileLastDay() {
        try {
            log.info("[RECON] Iniciando conciliación T-1");
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime from = now.minusDays(1);

            // Simplificación: detectar PAID sin factura y marcar pending o emitir si corresponde
            List<Transaction> recentPaid = transactionRepository.findAll().stream()
                    .filter(t -> t.getStatus() == TransactionStatus.PAID)
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().isAfter(from))
                    .toList();

            int billed = 0, pending = 0, errors = 0;
            for (Transaction tx : recentPaid) {
                try {
                    var settings = billingSettingsService.getActiveSettings(tx.getTenantId()).orElse(null);
                    if (settings == null) { continue; }
                    if (Boolean.TRUE.equals(settings.getRequireBillingConfirmation())) {
                        if (!"billed".equals(tx.getBillingStatus())) {
                            tx.setBillingStatus("pending");
                            transactionRepository.save(tx);
                            pending++;
                        }
                    } else {
                        if (tx.getInvoiceNumber() == null || tx.getInvoiceNumber().isEmpty()) {
                            invoiceService.createFacturaInFacturante(tx);
                            tx.setBillingStatus("billed");
                            transactionRepository.save(tx);
                            billed++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    log.warn("[RECON] Error conciliando tx {}: {}", tx.getExternalId(), e.getMessage());
                }
            }
            log.info("[RECON] T-1 completado: billed={}, pending={}, errors={}", billed, pending, errors);
        } catch (Exception e) {
            log.error("[RECON] Error general en conciliación T-1", e);
        }
    }
}


