package com.gf.connector.controllers;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.facturante.model.GetnetPaymentIntentResponse;
import com.gf.connector.facturante.model.GetnetRefundRequest;
import com.gf.connector.facturante.model.GetnetRefundResponse;
import com.gf.connector.facturante.service.GetnetService;
import com.gf.connector.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;

@Slf4j
@RestController
@RequestMapping("/api/getnet")
@RequiredArgsConstructor
public class GetnetController {
    
    private final GetnetService getnetService;
    private final TransactionRepository transactionRepository;
    
    /**
     * Crea un Payment Intent en GetNet para una transacción existente
     */
    @PostMapping("/payment-intent")
    public ResponseEntity<?> createPaymentIntent(@RequestBody CreatePaymentIntentRequest request) {
        try {
            // Buscar la transacción
            Transaction transaction = transactionRepository.findById(request.getTransactionId())
                    .orElseThrow(() -> new RuntimeException("Transacción no encontrada: " + request.getTransactionId()));
            
            // Crear Payment Intent en GetNet
            GetnetPaymentIntentResponse response = getnetService.createPaymentIntent(
                    transaction, 
                    request.getCustomerEmail(), 
                    request.getCustomerName(), 
                    request.getCustomerDoc()
            );
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Payment Intent creado exitosamente",
                    "payment_intent_id", response.getPaymentIntentId(),
                    "transaction_id", transaction.getId(),
                    "external_id", transaction.getExternalId()
            ));
            
        } catch (Exception e) {
            log.error("Error al crear Payment Intent", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error al crear Payment Intent: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Cancela un pago en GetNet
     */
    @PostMapping("/payments/{paymentId}/cancel")
    public ResponseEntity<?> cancelPayment(@PathVariable String paymentId) {
        try {
            getnetService.cancelPayment(paymentId);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Pago cancelado exitosamente",
                    "payment_id", paymentId
            ));
            
        } catch (Exception e) {
            log.error("Error al cancelar pago: {}", paymentId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error al cancelar pago: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Reembolsa un pago en GetNet
     */
    @PostMapping("/payments/{paymentId}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable String paymentId, 
                                         @RequestBody RefundPaymentRequest request) {
        try {
            GetnetRefundResponse response = getnetService.refundPayment(
                    paymentId, 
                    request.getAmount(), 
                    request.getReason()
            );
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Pago reembolsado exitosamente",
                    "payment_id", paymentId,
                    "refund_amount", request.getAmount(),
                    "refund_reason", request.getReason(),
                    "refund_id", response.getGeneratedBy()
            ));
            
        } catch (Exception e) {
            log.error("Error al reembolsar pago: {}", paymentId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error al reembolsar pago: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtiene el estado de una transacción en GetNet
     */
    @GetMapping("/transactions/{transactionId}/status")
    public ResponseEntity<?> getTransactionStatus(@PathVariable UUID transactionId) {
        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new RuntimeException("Transacción no encontrada: " + transactionId));
            
            return ResponseEntity.ok(Map.of(
                    "transaction_id", transaction.getId(),
                    "external_id", transaction.getExternalId(),
                    "status", transaction.getStatus(),
                    "amount", transaction.getAmount(),
                    "currency", transaction.getCurrency(),
                    "customer_doc", transaction.getCustomerDoc(),
                    "captured_at", transaction.getCapturedAt(),
                    "created_at", transaction.getCreatedAt(),
                    "updated_at", transaction.getUpdatedAt()
            ));
            
        } catch (Exception e) {
            log.error("Error al obtener estado de transacción: {}", transactionId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error al obtener estado de transacción: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Prueba la autenticación con GetNet
     */
    @PostMapping("/test-auth")
    public ResponseEntity<?> testGetnetAuth() {
        try {
            getnetService.testGetnetAuth();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Autenticación con GetNet exitosa"
            ));
        } catch (Exception e) {
            log.error("Error en autenticación con GetNet", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error en autenticación: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Obtiene el estado del token (debugging)
     */
    @GetMapping("/token-status")
    public ResponseEntity<?> getTokenStatus() {
        try {
            String status = getnetService.getTokenStatus();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "token_status", status
            ));
        } catch (Exception e) {
            log.error("Error al obtener estado del token", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error al obtener estado del token: " + e.getMessage()
            ));
        }
    }
    
    
    /**
     * Debug del estado interno del token (debugging avanzado)
     */
    @GetMapping("/debug-token")
    public ResponseEntity<?> debugToken() {
        try {
            String debugInfo = getnetService.debugTokenState();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "debug_info", debugInfo
            ));
        } catch (Exception e) {
            log.error("Error al debuggear token", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error al debuggear token: " + e.getMessage()
            ));
        }
    }
    
    /**
     * PRUEBA IRL COMPLETA - Simula el flujo completo de producción
     * Crea transacción + Payment Intent + Factura en un solo endpoint
     */
    @PostMapping("/flujo-completo")
    public ResponseEntity<?> testCompleteFlow(@RequestBody TestCompleteFlowRequest request) {
        try {
            log.info("=== INICIANDO PRUEBA IRL COMPLETA ===");
            
            // 1. Crear transacción en BD (simula webhook)
            Transaction transaction = Transaction.builder()
                    .externalId(null) // Se llenará con el Payment Intent
                    .amount(request.getAmount())
                    .currency("ARS")
                    .status(TransactionStatus.AUTHORIZED)
                    .customerDoc(request.getCustomerDoc())
                    .build();
            
            transaction = transactionRepository.save(transaction);
            
            // 2. Crear Payment Intent en GetNet
            GetnetPaymentIntentResponse paymentIntent = getnetService.createPaymentIntent(
                    transaction, 
                    request.getCustomerEmail(), 
                    request.getCustomerName(), 
                    request.getCustomerDoc()
            );
            log.info("✅ Payment Intent creado: ID={}", paymentIntent.getPaymentIntentId());
            
            // 3. Actualizar transacción con external_id
            transaction.setExternalId(paymentIntent.getPaymentIntentId());
            transaction.setStatus(TransactionStatus.PAID); // Simular pago completado
            transaction = transactionRepository.save(transaction);
            log.info("✅ Transacción actualizada con external_id: {}", transaction.getExternalId());
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("status", "success");
            result.put("message", "Flujo IRL completado exitosamente");
            result.put("transaction_id", transaction.getId());
            result.put("external_id", transaction.getExternalId());
            result.put("payment_intent_id", paymentIntent.getPaymentIntentId());
            result.put("amount", transaction.getAmount());
            result.put("currency", transaction.getCurrency());
            result.put("customer_doc", transaction.getCustomerDoc());
            
            
            log.info("=== PRUEBA IRL COMPLETA EXITOSA ===");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error en prueba IRL completa", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Error en flujo IRL: " + e.getMessage()
            ));
        }
    }
    
    // DTOs para las requests
    public static class CreatePaymentIntentRequest {
        private UUID transactionId;
        private String customerEmail;
        private String customerName;
        private String customerDoc;
        
        // Getters y setters
        public UUID getTransactionId() { return transactionId; }
        public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerDoc() { return customerDoc; }
        public void setCustomerDoc(String customerDoc) { this.customerDoc = customerDoc; }
    }
    
    public static class RefundPaymentRequest {
        private BigDecimal amount;
        private String reason;
        
        // Getters y setters
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
    
    public static class TestCompleteFlowRequest {
        private BigDecimal amount;
        private String customerEmail;
        private String customerName;
        private String customerDoc;
        
        // Getters y setters
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerDoc() { return customerDoc; }
        public void setCustomerDoc(String customerDoc) { this.customerDoc = customerDoc; }
    }
}
