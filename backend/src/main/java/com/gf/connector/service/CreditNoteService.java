package com.gf.connector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.domain.CreditNote;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.facturante.model.CrearComprobanteRequest;
import com.gf.connector.facturante.model.CrearComprobanteResponse;
import com.gf.connector.facturante.service.FacturanteService;
import com.gf.connector.repo.CreditNoteRepository;
import com.gf.connector.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final TransactionRepository transactionRepository;
    private final FacturanteService facturanteService;
    private final ObjectMapper objectMapper;
    private final BillingSettingsService billingSettingsService;

    /**
     * Procesa un reembolso y genera la nota de crédito según la estrategia configurada
     */
    @Transactional
    public CreditNote processRefund(Transaction transaction, String refundReason) {
        log.info("Procesando reembolso para transacción: {} con motivo: {}", 
                transaction.getExternalId(), refundReason);

        // Verificar que la transacción esté pagada
        if (transaction.getStatus() != TransactionStatus.PAID) {
            throw new IllegalArgumentException("Solo se pueden reembolsar transacciones pagadas");
        }

        // Verificar que no tenga nota de crédito ya
        Optional<CreditNote> existingCreditNote = creditNoteRepository.findByTransactionId(transaction.getId());
        if (existingCreditNote.isPresent()) {
            log.warn("La transacción {} ya tiene una nota de crédito: {}", 
                    transaction.getExternalId(), existingCreditNote.get().getId());
            return existingCreditNote.get();
        }

        // Obtener estrategia de configuración
        String strategy = getCreditNoteStrategy();
        log.info("Estrategia de nota de crédito: {}", strategy);

        // Crear nota de crédito según la estrategia
        CreditNote creditNote = createCreditNote(transaction, refundReason, strategy);

        // Actualizar transacción
        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setRefundReason(refundReason);
        transaction.setRefundedAt(OffsetDateTime.now());
        transaction.setCreditNoteNumber(creditNote.getCreditNoteNumber());
        transaction.setCreditNoteCae(creditNote.getCreditNoteCae());
        transaction.setCreditNoteStatus(creditNote.getStatus());
        transaction.setCreditNoteStrategy(creditNote.getStrategy());
        
        transactionRepository.save(transaction);

        return creditNote;
    }

    /**
     * Crea una nota de crédito según la estrategia especificada
     */
    private CreditNote createCreditNote(Transaction transaction, String refundReason, String strategy) {
        CreditNote creditNote = CreditNote.builder()
                .transaction(transaction)
                .refundReason(refundReason)
                .strategy(strategy)
                .status("pending")
                .build();

        creditNote = creditNoteRepository.save(creditNote);

        try {
            switch (strategy.toLowerCase()) {
                case "automatic":
                    return createAutomaticCreditNote(creditNote, transaction);
                case "manual":
                    return createManualCreditNote(creditNote, transaction);
                case "stub":
                    return createStubCreditNote(creditNote, transaction);
                default:
                    log.warn("Estrategia desconocida: {}, usando stub", strategy);
                    return createStubCreditNote(creditNote, transaction);
            }
        } catch (Exception e) {
            log.error("Error al crear nota de crédito con estrategia {}: {}", strategy, e.getMessage(), e);
            creditNote.setStatus("error");
            creditNote.setResponseJson("{\"error\": \"" + e.getMessage() + "\"}");
            return creditNoteRepository.save(creditNote);
        }
    }

    /**
     * Crea nota de crédito automática en Facturante
     */
    private CreditNote createAutomaticCreditNote(CreditNote creditNote, Transaction transaction) {
        log.info("Creando nota de crédito automática para transacción: {}", transaction.getExternalId());

        try {
            // Generar request de nota de crédito
            CrearComprobanteRequest request = buildCreditNoteRequest(transaction);
            creditNote.setRequestJson(objectMapper.writeValueAsString(request));
            creditNoteRepository.save(creditNote);

            // Llamar a Facturante
            CrearComprobanteResponse response = facturanteService.crearNotaCredito(transaction);

            if (response.getExitoso()) {
                creditNote.setStatus("sent");
                creditNote.setCreditNoteNumber(response.getNumeroComprobante());
                creditNote.setCreditNoteCae(response.getCae());
                creditNote.setPdfUrl(response.getPdfUrl());
                log.info("Nota de crédito creada exitosamente: Número={}, CAE={}", 
                        response.getNumeroComprobante(), response.getCae());
            } else {
                creditNote.setStatus("error");
                log.error("Error al crear nota de crédito: {}", String.join(", ", response.getMensajes()));
            }

            creditNote.setResponseJson(objectMapper.writeValueAsString(response));

        } catch (Exception e) {
            log.error("Error al crear nota de crédito automática", e);
            creditNote.setStatus("error");
            creditNote.setResponseJson("{\"error\": \"Error técnico: " + e.getMessage() + "\"}");
        }

        return creditNoteRepository.save(creditNote);
    }

    /**
     * Crea nota de crédito manual (pendiente de procesamiento manual)
     */
    private CreditNote createManualCreditNote(CreditNote creditNote, Transaction transaction) {
        log.info("Creando nota de crédito manual para transacción: {}", transaction.getExternalId());

        creditNote.setStatus("pending");
        creditNote.setResponseJson("{\"message\": \"Nota de crédito pendiente de procesamiento manual\"}");
        
        return creditNoteRepository.save(creditNote);
    }

    /**
     * Crea nota de crédito stub (simulada)
     */
    private CreditNote createStubCreditNote(CreditNote creditNote, Transaction transaction) {
        log.info("Creando nota de crédito stub para transacción: {}", transaction.getExternalId());

        // Generar número de nota de crédito stub
        String stubNumber = "NC-STUB-" + System.currentTimeMillis();
        String stubCae = "STUB-CAE-" + System.currentTimeMillis();

        creditNote.setStatus("stub");
        creditNote.setCreditNoteNumber(stubNumber);
        creditNote.setCreditNoteCae(stubCae);
        creditNote.setResponseJson("{\"message\": \"Nota de crédito simulada\", \"stub\": true}");

        log.info("Nota de crédito stub creada: Número={}, CAE={}", stubNumber, stubCae);
        
        return creditNoteRepository.save(creditNote);
    }

    /**
     * Construye el request para nota de crédito en Facturante
     */
    private CrearComprobanteRequest buildCreditNoteRequest(Transaction transaction) {
        // TODO: Implementar construcción específica para notas de crédito
        // Por ahora, usar el mismo formato que las facturas pero con tipo de comprobante diferente
        return facturanteService.buildCreditNoteRequest(transaction);
    }

    /**
     * Obtiene la estrategia de nota de crédito desde la configuración
     */
    private String getCreditNoteStrategy() {
        return billingSettingsService.getActiveSettings()
                .map(settings -> settings.getCreditNoteStrategy())
                .orElse("stub"); // Default a stub si no hay configuración
    }

    /**
     * Busca nota de crédito por ID de transacción
     */
    public Optional<CreditNote> findByTransactionId(UUID transactionId) {
        return creditNoteRepository.findByTransactionId(transactionId);
    }

    /**
     * Busca nota de crédito por número
     */
    public Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber) {
        return creditNoteRepository.findByCreditNoteNumber(creditNoteNumber);
    }

    /**
     * Busca una nota de crédito por su ID
     */
    public Optional<CreditNote> findById(UUID creditNoteId) {
        return creditNoteRepository.findById(creditNoteId);
    }

    /**
     * Procesa manualmente una nota de crédito pendiente
     */
    @Transactional
    public CreditNote processManualCreditNote(UUID creditNoteId) {
        CreditNote creditNote = creditNoteRepository.findById(creditNoteId)
                .orElseThrow(() -> new IllegalArgumentException("Nota de crédito no encontrada"));

        if (!"pending".equals(creditNote.getStatus())) {
            throw new IllegalArgumentException("Solo se pueden procesar notas de crédito pendientes");
        }

        return createAutomaticCreditNote(creditNote, creditNote.getTransaction());
    }
}
