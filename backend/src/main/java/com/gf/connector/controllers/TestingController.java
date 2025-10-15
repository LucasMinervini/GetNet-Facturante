package com.gf.connector.controllers;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/testing")
@RequiredArgsConstructor
@Tag(name = "Testing", description = "Endpoints para testing de integración")
public class TestingController {
    
    private final TransactionRepository transactionRepository;
    private final InvoiceService invoiceService;
    
    /**
     * Crea una transacción de prueba y la factura automáticamente
     */
    @PostMapping("/create-test-transaction")
    @Operation(summary = "Crear transacción de prueba", description = "Crea una transacción simulada y la factura en Facturante")
    public ResponseEntity<?> createTestTransaction() {
        try {
            log.info("=== INICIANDO FLUJO DE TESTING COMPLETO ===");
            
            // 1. Crear transacción de prueba
            Transaction testTransaction = Transaction.builder()
                    .externalId("TEST-" + System.currentTimeMillis())
                    .amount(new BigDecimal("1000.00"))
                    .currency("ARS")
                    .status(TransactionStatus.PAID)
                    .customerDoc("20123456789")
                    .capturedAt(OffsetDateTime.now())
                    .reconciled(false)
                    .billingStatus("pending")
                    .build();
            
            testTransaction = transactionRepository.save(testTransaction);
            log.info("✅ Transacción de prueba creada: ID={}, ExternalID={}", 
                    testTransaction.getId(), testTransaction.getExternalId());
            
            // 2. Crear factura en Facturante
            log.info("🔄 Creando factura en Facturante...");
            var invoice = invoiceService.createFacturaInFacturante(testTransaction);
            
            // 3. Verificar resultado
            if ("sent".equals(invoice.getStatus())) {
                log.info("✅ Factura creada exitosamente en Facturante");
                log.info("📄 PDF URL: {}", invoice.getPdfUrl());
                log.info("📋 Número de factura: {}", testTransaction.getInvoiceNumber());
                log.info("🔢 CAE: {}", testTransaction.getCae());
                
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Flujo de testing completado exitosamente",
                        "transactionId", testTransaction.getId(),
                        "externalId", testTransaction.getExternalId(),
                        "invoiceNumber", testTransaction.getInvoiceNumber(),
                        "cae", testTransaction.getCae(),
                        "pdfUrl", invoice.getPdfUrl(),
                        "downloadUrl", "/api/invoices/pdf/" + testTransaction.getId()
                ));
            } else {
                log.error("❌ Error al crear factura en Facturante: {}", invoice.getResponseJson());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Error al crear factura en Facturante",
                        "error", invoice.getResponseJson()
                ));
            }
            
        } catch (Exception e) {
            log.error("❌ Error en flujo de testing", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error en flujo de testing: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Verifica el estado de una transacción de prueba
     */
    @GetMapping("/check-transaction/{transactionId}")
    @Operation(summary = "Verificar transacción de prueba", description = "Verifica el estado de una transacción de prueba")
    public ResponseEntity<?> checkTestTransaction(@PathVariable UUID transactionId) {
        try {
            var transaction = transactionRepository.findById(transactionId);
            if (transaction.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            var tx = transaction.get();
            return ResponseEntity.ok(Map.of(
                    "transactionId", tx.getId(),
                    "externalId", tx.getExternalId(),
                    "status", tx.getStatus(),
                    "amount", tx.getAmount(),
                    "invoiceNumber", tx.getInvoiceNumber(),
                    "cae", tx.getCae(),
                    "billingStatus", tx.getBillingStatus(),
                    "downloadUrl", "/api/invoices/pdf/" + tx.getId()
            ));
            
        } catch (Exception e) {
            log.error("Error al verificar transacción", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Error al verificar transacción: " + e.getMessage()
            ));
        }
    }
}
