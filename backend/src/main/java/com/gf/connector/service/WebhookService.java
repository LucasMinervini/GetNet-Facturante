package com.gf.connector.service;

import com.gf.connector.domain.CreditNote;
import com.gf.connector.domain.Invoice;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.domain.WebhookEvent;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.repo.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Servicio principal para el procesamiento de webhooks de Getnet.
 * 
 * Responsabilidades:
 * 1. Persistir eventos de webhook para auditoría
 * 2. Transformar payloads de Getnet a transacciones del dominio
 * 3. Generar automáticamente facturas para transacciones pagadas
 * 4. Manejar errores y logging detallado
 */
@Service
@RequiredArgsConstructor
public class WebhookService {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookEventRepository webhookEventRepository;
    private final TransactionRepository transactionRepository;
    private final GetnetToFacturanteTransformationService transformationService;
    private final InvoiceService invoiceService;
    private final CreditNoteService creditNoteService;
    private final BillingSettingsService billingSettingsService;
    private static final java.util.UUID DEFAULT_TEST_TENANT = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");

    /**
     * Procesa un payload de webhook de Getnet de forma completa:
     * 1. Valida y persiste el evento
     * 2. Transforma los datos usando el servicio especializado
     * 3. Crea o actualiza la transacción
     * 4. Genera factura automáticamente si la transacción está pagada
     */
    @Transactional
    public WebhookProcessingResult processGetnetPayload(String rawJson, Map<String, Object> payload, java.util.UUID tenantId) {
        WebhookEvent webhookEvent = null;
        Transaction transaction = null;
        Invoice invoice = null;
        CreditNote creditNote = null;
        
        try {
            log.info("Iniciando procesamiento de webhook de Getnet");
            
            // 1. Persistir evento de webhook para auditoría con idempotencia
            String eventHash = sha256Hex(rawJson);
            // Intentar encontrar un evento existente con el mismo hash
            WebhookEvent existingEvent = null;
            try {
                existingEvent = webhookEventRepository.findAll().stream()
                        .filter(e -> eventHash.equals(e.getEventHash()))
                        .findFirst().orElse(null);
            } catch (Exception ignored) {}

            if (existingEvent != null && Boolean.TRUE.equals(existingEvent.isProcessed())) {
                log.info("Evento duplicado detectado (idempotente), omitiendo procesamiento");
                return WebhookProcessingResult.builder()
                        .success(true)
                        .webhookEvent(existingEvent)
                        .transaction(null)
                        .invoice(null)
                        .message("Duplicate webhook ignored by idempotency")
                        .build();
            }

            webhookEvent = WebhookEvent.builder()
                    .provider("getnet")
                    .payload(rawJson)
                    .eventHash(eventHash)
                    .processed(false)
                    .build();
            webhookEvent = webhookEventRepository.save(webhookEvent);
            log.info("Evento de webhook persistido con ID: {}", webhookEvent.getId());

            // 2. Transformar payload a transacción usando servicio especializado
            transaction = transformationService.transformWebhookToTransaction(rawJson, payload);
            transaction.setTenantId(tenantId);
            log.info("Payload transformado a transacción: ID={}, Amount={}, Status={}", 
                    transaction.getExternalId(), transaction.getAmount(), transaction.getStatus());

            // 3. Buscar transacción existente o crear nueva
            Transaction existingTransaction = transactionRepository.findByExternalId(transaction.getExternalId())
                    .orElse(null);
            
            if (existingTransaction != null) {
                // Actualizar transacción existente
                log.info("Actualizando transacción existente: {}", existingTransaction.getId());
                existingTransaction.setAmount(transaction.getAmount());
                existingTransaction.setCurrency(transaction.getCurrency());
                existingTransaction.setStatus(transaction.getStatus());
                existingTransaction.setCustomerDoc(transaction.getCustomerDoc());
                existingTransaction.setTenantId(tenantId);
                transaction = transactionRepository.save(existingTransaction);
            } else {
                // Crear nueva transacción
                log.info("Creando nueva transacción: {}", transaction.getExternalId());
                transaction = transactionRepository.save(transaction);
            }

            // 4. Determinar si generar factura automáticamente o marcar para confirmación
            if (shouldGenerateInvoice(transaction)) {
                var settings = billingSettingsService.getActiveSettings(tenantId).orElse(null);
                
                if (settings != null && Boolean.TRUE.equals(settings.getRequireBillingConfirmation())) {
                    // Marcar para confirmación manual
                    log.info("Marcando transacción {} para confirmación de facturación", transaction.getExternalId());
                    transaction.setBillingStatus("pending");
                    transaction = transactionRepository.save(transaction);
                } else {
                    // Generar factura automáticamente (comportamiento actual)
                    log.info("Generando factura automáticamente para transacción pagada: {}", transaction.getExternalId());
                    try {
                        invoice = invoiceService.createFacturaInFacturante(transaction);
                        transaction.setBillingStatus("billed");
                        log.info("Factura generada exitosamente: Status={}, CAE={}", 
                                invoice.getStatus(), transaction.getCae());
                    } catch (Exception e) {
                        log.error("Error al generar factura automática para transacción {}: {}", 
                                transaction.getExternalId(), e.getMessage(), e);
                        transaction.setBillingStatus("error");
                        // No fallar el procesamiento del webhook por error de facturación
                    }
                    transaction = transactionRepository.save(transaction);
                }
            } else {
                // Si no debe generar factura, marcar como no aplicable
                transaction.setBillingStatus("not_applicable");
                transaction = transactionRepository.save(transaction);
            }
            
            // 5. Procesar reembolso si la transacción está reembolsada
            if (transaction.getStatus() == TransactionStatus.REFUNDED && shouldProcessRefund(transaction)) {
                log.info("Procesando reembolso automáticamente para transacción: {}", transaction.getExternalId());
                try {
                    String refundReason = extractRefundReason(payload);
                    creditNote = creditNoteService.processRefund(transaction, refundReason);
                    log.info("Nota de crédito generada exitosamente: Status={}, Strategy={}", 
                            creditNote.getStatus(), creditNote.getStrategy());
                } catch (Exception e) {
                    log.error("Error al procesar reembolso automático para transacción {}: {}", 
                            transaction.getExternalId(), e.getMessage(), e);
                    // No fallar el procesamiento del webhook por error de reembolso
                }
            }

            // 5. Marcar evento como procesado
            webhookEvent.setProcessed(true);
            webhookEventRepository.save(webhookEvent);
            
            log.info("Webhook procesado exitosamente: Transaction={}, Invoice={}", 
                    transaction.getId(), invoice != null ? invoice.getId() : "N/A");
            
            return WebhookProcessingResult.builder()
                    .success(true)
                    .webhookEvent(webhookEvent)
                    .transaction(transaction)
                    .invoice(invoice)
                    .message("Webhook procesado exitosamente")
                    .build();
            
        } catch (Exception e) {
            log.error("Error al procesar webhook de Getnet", e);
            
            // Marcar evento como no procesado en caso de error
            if (webhookEvent != null) {
                webhookEvent.setProcessed(false);
                webhookEventRepository.save(webhookEvent);
            }
            
            return WebhookProcessingResult.builder()
                    .success(false)
                    .webhookEvent(webhookEvent)
                    .transaction(transaction)
                    .invoice(invoice)
                    .message("Error al procesar webhook: " + e.getMessage())
                    .error(e)
                    .build();
        }
    }

    // Overload para tests (sin tenantId expl cito)
    public WebhookProcessingResult processGetnetPayload(String rawJson, Map<String, Object> payload) {
        return processGetnetPayload(rawJson, payload, DEFAULT_TEST_TENANT);
    }

    private static String sha256Hex(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((data != null ? data : "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error computing SHA-256", e);
        }
    }
    
    /**
     * Determina si se debe generar una factura automáticamente
     */
    private boolean shouldGenerateInvoice(Transaction transaction) {
        // Obtener configuración activa
        var settings = billingSettingsService.getActiveSettings(transaction.getTenantId()).orElse(null);
        
        // Aplicar regla de facturar solo PAID si está configurada
        if (settings != null && Boolean.TRUE.equals(settings.getFacturarSoloPaid())) {
            if (transaction.getStatus() != TransactionStatus.PAID) {
                log.info("Transacción {} no se factura automáticamente (solo PAID): Status={}", 
                        transaction.getExternalId(), transaction.getStatus());
                return false;
            }
        } else {
            // Fallback: solo generar factura para transacciones pagadas
            if (transaction.getStatus() != TransactionStatus.PAID) {
                return false;
            }
        }
        
        // No generar si ya tiene número de factura
        if (transaction.getInvoiceNumber() != null && !transaction.getInvoiceNumber().isEmpty()) {
            log.info("Transacción {} ya tiene factura: {}", 
                    transaction.getExternalId(), transaction.getInvoiceNumber());
            return false;
        }
        
        return true;
    }
    
    /**
     * Determina si se debe procesar un reembolso automáticamente
     */
    private boolean shouldProcessRefund(Transaction transaction) {
        // Solo procesar si no tiene nota de crédito ya
        if (transaction.getCreditNoteNumber() != null && !transaction.getCreditNoteNumber().isEmpty()) {
            log.info("Transacción {} ya tiene nota de crédito: {}", 
                    transaction.getExternalId(), transaction.getCreditNoteNumber());
            return false;
        }
        
        // Solo procesar si la transacción estaba pagada previamente
        if (transaction.getInvoiceNumber() == null || transaction.getInvoiceNumber().isEmpty()) {
            log.info("Transacción {} no tiene factura, no se procesa reembolso: {}", 
                    transaction.getExternalId());
            return false;
        }
        
        return true;
    }
    
    /**
     * Extrae el motivo del reembolso del payload del webhook
     */
    private String extractRefundReason(Map<String, Object> payload) {
        try {
            // Intentar extraer de diferentes ubicaciones posibles en el payload
            if (payload.containsKey("refund_reason")) {
                return String.valueOf(payload.get("refund_reason"));
            }
            
            if (payload.containsKey("reason")) {
                return String.valueOf(payload.get("reason"));
            }
            
            if (payload.containsKey("description")) {
                return String.valueOf(payload.get("description"));
            }
            
            // Si no se encuentra, usar motivo por defecto
            return "Reembolso solicitado por el cliente";
            
        } catch (Exception e) {
            log.warn("Error al extraer motivo de reembolso del payload", e);
            return "Reembolso solicitado por el cliente";
        }
    }
    
    /**
     * Resultado del procesamiento de webhook
     */
    public static class WebhookProcessingResult {
        private final boolean success;
        private final WebhookEvent webhookEvent;
        private final Transaction transaction;
        private final Invoice invoice;
        private final String message;
        private final Exception error;
        
        private WebhookProcessingResult(boolean success, WebhookEvent webhookEvent, 
                                      Transaction transaction, Invoice invoice, 
                                      String message, Exception error) {
            this.success = success;
            this.webhookEvent = webhookEvent;
            this.transaction = transaction;
            this.invoice = invoice;
            this.message = message;
            this.error = error;
        }
        
        public static WebhookProcessingResultBuilder builder() {
            return new WebhookProcessingResultBuilder();
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public WebhookEvent getWebhookEvent() { return webhookEvent; }
        public Transaction getTransaction() { return transaction; }
        public Invoice getInvoice() { return invoice; }
        public String getMessage() { return message; }
        public Exception getError() { return error; }
        
        public static class WebhookProcessingResultBuilder {
            private boolean success;
            private WebhookEvent webhookEvent;
            private Transaction transaction;
            private Invoice invoice;
            private String message;
            private Exception error;
            
            public WebhookProcessingResultBuilder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public WebhookProcessingResultBuilder webhookEvent(WebhookEvent webhookEvent) {
                this.webhookEvent = webhookEvent;
                return this;
            }
            
            public WebhookProcessingResultBuilder transaction(Transaction transaction) {
                this.transaction = transaction;
                return this;
            }
            
            public WebhookProcessingResultBuilder invoice(Invoice invoice) {
                this.invoice = invoice;
                return this;
            }
            
            public WebhookProcessingResultBuilder message(String message) {
                this.message = message;
                return this;
            }
            
            public WebhookProcessingResultBuilder error(Exception error) {
                this.error = error;
                return this;
            }
            
            public WebhookProcessingResult build() {
                return new WebhookProcessingResult(success, webhookEvent, transaction, invoice, message, error);
            }
        }
    }
}
