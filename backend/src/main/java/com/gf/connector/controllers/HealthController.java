package com.gf.connector.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.TransactionRepository;

import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@RestController
public class HealthController {
    
    @Autowired
    private TransactionRepository transactionRepository;
    
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
    
    @PostMapping("/api/test-flow")
    public Map<String, Object> testFlow() {
        try {
            // Crear una transacción REAL en la base de datos
            Transaction transaction = new Transaction();
            transaction.setId(UUID.randomUUID());
            transaction.setExternalId("TEST-" + System.currentTimeMillis());
            transaction.setAmount(new BigDecimal("1000.00"));
            transaction.setCurrency("ARS");
            transaction.setStatus(TransactionStatus.PAID);
            transaction.setCreatedAt(OffsetDateTime.now());
            transaction.setUpdatedAt(OffsetDateTime.now());
            
            // Guardar en la BD
            Transaction savedTransaction = transactionRepository.save(transaction);
            
            return Map.of(
                "status", "success",
                "message", "Transacción REAL creada exitosamente en la BD",
                "transaction_id", savedTransaction.getId().toString(),
                "external_id", savedTransaction.getExternalId(),
                "status_transaction", savedTransaction.getStatus().toString(),
                "amount", savedTransaction.getAmount(),
                "currency", savedTransaction.getCurrency(),
                "steps_completed", java.util.List.of(
                    "✅ Transacción REAL creada en la BD",
                    "✅ Estado configurado como PAID",
                    "✅ Listo para probar Payment Intent"
                ),
                "next_step", "Usar este transaction_id para probar /api/getnet/payment-intent"
            );
            
        } catch (Exception e) {
            return Map.of(
                "status", "error",
                "message", "Error al crear transacción: " + e.getMessage(),
                "error_type", e.getClass().getSimpleName()
            );
        }
    }
}
