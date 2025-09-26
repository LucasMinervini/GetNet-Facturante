package com.gf.connector.facturante.service;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.facturante.client.GetnetClient;
import com.gf.connector.facturante.model.*;
import com.gf.connector.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetnetService {
    
    private final GetnetClient getnetClient;
    private final TransactionRepository transactionRepository;
    
    /**
     * Crea un Payment Intent en GetNet y lo asocia con una transacción local
     */
    public GetnetPaymentIntentResponse createPaymentIntent(Transaction transaction, 
                                                         String customerEmail, 
                                                         String customerName,
                                                         String customerDoc) {
        try {
            // Convertir la transacción a formato GetNet
            GetnetPaymentIntent paymentIntent = buildPaymentIntent(transaction, customerEmail, customerName, customerDoc);
            
            // Crear Payment Intent en GetNet
            GetnetPaymentIntentResponse response = getnetClient.createPaymentIntent(paymentIntent);
            
            // Actualizar la transacción con el ID del Payment Intent
            transaction.setExternalId(response.getPaymentIntentId());
            transactionRepository.save(transaction);
            
            log.info("Payment Intent creado exitosamente para transacción {}: {}", 
                    transaction.getId(), response.getPaymentIntentId());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error al crear Payment Intent para transacción: {}", transaction.getId(), e);
            throw new RuntimeException("Error al crear Payment Intent en GetNet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Procesa un webhook de GetNet y actualiza la transacción correspondiente
     */
    public void processWebhook(GetnetWebhookPayload webhookPayload) {
        try {
            log.info("Procesando webhook de GetNet para Payment Intent: {}", 
                    webhookPayload.getPaymentIntentId());
            
            // Buscar la transacción por el Payment Intent ID
            Transaction transaction = transactionRepository.findByExternalId(webhookPayload.getPaymentIntentId())
                    .orElseThrow(() -> new RuntimeException("Transacción no encontrada para Payment Intent: " + 
                            webhookPayload.getPaymentIntentId()));
            
            // Actualizar el estado de la transacción según el webhook
            updateTransactionFromWebhook(transaction, webhookPayload);
            
            // Guardar la transacción actualizada
            transactionRepository.save(transaction);
            
            log.info("Transacción {} actualizada exitosamente desde webhook de GetNet. Nuevo estado: {}", 
                    transaction.getId(), transaction.getStatus());
            
        } catch (Exception e) {
            log.error("Error al procesar webhook de GetNet", e);
            throw new RuntimeException("Error al procesar webhook de GetNet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancela un pago en GetNet
     */
    public void cancelPayment(String paymentId) {
        try {
            getnetClient.cancelPayment(paymentId);
            
            // Buscar y actualizar la transacción local
            Transaction transaction = transactionRepository.findByExternalId(paymentId)
                    .orElseThrow(() -> new RuntimeException("Transacción no encontrada para pago: " + paymentId));
            
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            
            log.info("Pago cancelado exitosamente en GetNet y transacción actualizada: {}", paymentId);
            
        } catch (Exception e) {
            log.error("Error al cancelar pago: {}", paymentId, e);
            throw new RuntimeException("Error al cancelar pago: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reembolsa un pago en GetNet
     */
    public GetnetRefundResponse refundPayment(String paymentId, BigDecimal amount, String reason) {
        try {
            // Convertir el monto a centavos (formato requerido por GetNet)
            int amountInCents = amount.multiply(BigDecimal.valueOf(100)).intValue();
            
            GetnetRefundRequest refundRequest = GetnetRefundRequest.builder()
                    .amount(amountInCents)
                    .build();
            
            // Procesar reembolso en GetNet
            GetnetRefundResponse response = getnetClient.refundPayment(paymentId, refundRequest);
            
            // Buscar y actualizar la transacción local
            Transaction transaction = transactionRepository.findByExternalId(paymentId)
                    .orElseThrow(() -> new RuntimeException("Transacción no encontrada para pago: " + paymentId));
            
            transaction.setStatus(TransactionStatus.REFUNDED);
            transaction.setRefundReason(reason);
            transaction.setRefundedAt(OffsetDateTime.now());
            transactionRepository.save(transaction);
            
            log.info("Pago reembolsado exitosamente en GetNet: {} - Monto: {}", paymentId, amount);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error al reembolsar pago: {}", paymentId, e);
            throw new RuntimeException("Error al reembolsar pago: " + e.getMessage(), e);
        }
    }
    
    /**
     * Construye un Payment Intent de GetNet a partir de una transacción local
     */
    private GetnetPaymentIntent buildPaymentIntent(Transaction transaction, 
                                                 String customerEmail, 
                                                 String customerName,
                                                 String customerDoc) {
        
        // Convertir el monto a centavos
        int amountInCents = transaction.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
        
        // Construir el cliente
        GetnetPaymentIntent.GetnetCustomer customer = GetnetPaymentIntent.GetnetCustomer.builder()
                .customerId(transaction.getCustomerDoc())
                .firstName(customerName.split(" ")[0])
                .lastName(customerName.contains(" ") ? 
                        customerName.substring(customerName.indexOf(" ") + 1) : "")
                .name(customerName)
                .email(customerEmail)
                .documentType("DNI") // Ajustar según tu país
                .documentNumber(customerDoc)
                .checkedEmail(false)
                .build();
        
        // Construir el pago
        GetnetPaymentIntent.GetnetPayment payment = GetnetPaymentIntent.GetnetPayment.builder()
                .currency(transaction.getCurrency())
                .amount(amountInCents)
                .build();
        
        // Construir productos (producto genérico)
        GetnetPaymentIntent.GetnetProduct product = GetnetPaymentIntent.GetnetProduct.builder()
                .productType("physical_goods")
                .title("Producto/Servicio")
                .description("Transacción " + transaction.getId())
                .value(amountInCents)
                .quantity(1)
                .build();
        
        return GetnetPaymentIntent.builder()
                .mode("instant")
                .orderId(transaction.getId().toString())
                .payment(payment)
                .products(List.of(product))
                .customer(customer)
                .pickupStore(false)
                .build();
    }
    
    /**
     * Actualiza una transacción local basándose en el webhook de GetNet
     */
    private void updateTransactionFromWebhook(Transaction transaction, GetnetWebhookPayload webhookPayload) {
        GetnetWebhookPayload.GetnetPaymentResult result = webhookPayload.getPayment().getResult();
        
        // Mapear el estado de GetNet a tu enum local
        TransactionStatus newStatus = mapGetnetStatusToLocalStatus(result.getStatus());
        transaction.setStatus(newStatus);
        
        // Si está autorizado, marcar como capturado
        if (newStatus == TransactionStatus.AUTHORIZED) {
            transaction.setCapturedAt(result.getTransactionDatetime());
        }
        
        // Actualizar timestamp
        transaction.setUpdatedAt(OffsetDateTime.now());
        
        log.info("Transacción {} actualizada: estado {} -> {}", 
                transaction.getId(), transaction.getStatus(), newStatus);
    }
    
    /**
     * Mapea los estados de GetNet a los estados locales
     */
    private TransactionStatus mapGetnetStatusToLocalStatus(String getnetStatus) {
        if (getnetStatus == null) {
            return TransactionStatus.FAILED;
        }
        
        switch (getnetStatus.toUpperCase()) {
            case "AUTHORIZED":
                return TransactionStatus.AUTHORIZED;
            case "DENIED":
                return TransactionStatus.FAILED;
            default:
                log.warn("Estado de GetNet no reconocido: {}", getnetStatus);
                return TransactionStatus.FAILED;
        }
    }

    /**
     * Prueba la autenticación con GetNet
     */
    public void testGetnetAuth() {
        log.info("Probando autenticación con GetNet...");
        String token = getnetClient.testAuth();
        log.info("Autenticación con GetNet exitosa");
    }
    
    /**
     * Obtiene el estado del token (debugging)
     */
    public String getTokenStatus() {
        return getnetClient.getTokenStatus();
    }
    
    /**
     * Fuerza la renovación del token (debugging)
     */
    public String forceTokenRefresh() {
        return getnetClient.forceTokenRefresh();
    }
    
    /**
     * Debug del estado interno del token (debugging avanzado)
     */
    public String debugTokenState() {
        return getnetClient.debugTokenState();
    }
}
