package com.gf.connector.service;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.security.GetnetAuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para la reconciliación de transacciones entre Getnet y la base de datos local
 * Garantiza que ninguna transacción PAID de Getnet se quede sin facturar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final GetnetAuthenticationService getnetAuthService;
    private final InvoiceService invoiceService;
    
    // NotificationService es opcional - solo existe si está configurado el email
    @Autowired(required = false)
    private NotificationService notificationService;

    /**
     * Ejecuta el proceso completo de reconciliación para un tenant específico
     * @param tenantId ID del tenant
     * @param startDate Fecha de inicio para el reporte
     * @param endDate Fecha de fin para el reporte
     * @return Resultado de la reconciliación
     */
    @Transactional
    public ReconciliationResult performReconciliation(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        log.info("Iniciando reconciliación para tenant {} desde {} hasta {}", tenantId, startDate, endDate);
        
        try {
            // 1. Obtener transacciones PAID de Getnet
            List<GetnetTransaction> getnetTransactions = getGetnetPaidTransactions(tenantId, startDate, endDate);
            log.info("Obtenidas {} transacciones PAID de Getnet", getnetTransactions.size());
            
            // 2. Obtener transacciones locales
            List<Transaction> localTransactions = getLocalTransactions(tenantId, startDate, endDate);
            log.info("Obtenidas {} transacciones locales", localTransactions.size());
            
            // 3. Identificar transacciones huérfanas (PAID en Getnet pero no facturadas localmente)
            List<GetnetTransaction> orphanTransactions = findOrphanTransactions(getnetTransactions, localTransactions);
            log.info("Encontradas {} transacciones huérfanas", orphanTransactions.size());
            
            // 4. Procesar transacciones huérfanas
            ReconciliationResult result = processOrphanTransactions(orphanTransactions, tenantId);
            
            // 5. Notificaciones
            if (notificationService != null) {
                if (result.hasErrors()) {
                    notificationService.sendReconciliationErrorNotification(tenantId, result);
                } else if (result.getProcessedCount() > 0) {
                    notificationService.sendReconciliationInfoNotification(tenantId, result.getProcessedCount());
                }
            }
            
            log.info("Reconciliación completada: {} procesadas, {} errores", 
                    result.getProcessedCount(), result.getErrorCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error durante la reconciliación para tenant {}: {}", tenantId, e.getMessage(), e);
            if (notificationService != null) {
                notificationService.sendReconciliationErrorNotification(tenantId, e);
            }
            throw new RuntimeException("Error en reconciliación", e);
        }
    }
    
    /**
     * Obtiene transacciones PAID de Getnet usando Merchant Reporting
     */
    private List<GetnetTransaction> getGetnetPaidTransactions(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        try {
            String startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String endDateStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            Map<String, Object> response = getnetAuthService.getMerchantReport(tenantId, startDateStr, endDateStr);
            if (response == null || response.isEmpty()) {
                log.warn("Respuesta vaca de Merchant Reporting");
                return List.of();
            }

            Object txs = response.get("transactions");
            if (!(txs instanceof List<?> list)) {
                log.warn("Formato inesperado en Merchant Reporting: sin 'transactions'");
                return List.of();
            }

            return list.stream()
                .filter(item -> item instanceof Map)
                .map(item -> (Map<?, ?>) item)
                .map(m -> {
                    Object idObj = m.get("id");
                    String id = idObj == null ? "" : String.valueOf(idObj);
                    Object statusObj = m.get("status");
                    String status = statusObj == null ? "" : String.valueOf(statusObj);
                    Double amount = null;
                    try {
                        Object amt = m.get("amount");
                        amount = amt == null ? null : Double.valueOf(String.valueOf(amt));
                    } catch (Exception ignore) {}
                    Object tsObj = m.get("timestamp");
                    String timestamp = tsObj == null ? "" : String.valueOf(tsObj);
                    return new GetnetTransaction(id, status, amount == null ? 0.0 : amount, timestamp);
                })
                .filter(tx -> "PAID".equalsIgnoreCase(tx.getStatus()))
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("Error al obtener reporte de Getnet: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Obtiene transacciones locales de la base de datos
     */
    private List<Transaction> getLocalTransactions(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        OffsetDateTime start = startDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime end = endDate.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);
        
        if (tenantId != null) {
            return transactionRepository.findByTenantIdAndCreatedAtBetween(tenantId, start, end);
        }
        return transactionRepository.findByCreatedAtBetween(start, end);
    }
    
    /**
     * Identifica transacciones huérfanas comparando Getnet con local
     */
    private List<GetnetTransaction> findOrphanTransactions(List<GetnetTransaction> getnetTransactions, 
                                                          List<Transaction> localTransactions) {
        
        // Crear mapa de transacciones locales por externalId (ID de Getnet)
        Map<String, Transaction> localByExternalId = localTransactions.stream()
            .filter(t -> t.getExternalId() != null)
            .collect(Collectors.toMap(
                Transaction::getExternalId,
                t -> t,
                (existing, replacement) -> existing
            ));
        
        // Encontrar transacciones de Getnet que no están en local o no están facturadas
        return getnetTransactions.stream()
            .filter(getnetTx -> {
                Transaction localTx = localByExternalId.get(getnetTx.getId());
                return localTx == null || 
                       localTx.getBillingStatus() == null || 
                       !"billed".equals(localTx.getBillingStatus());
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Procesa transacciones huérfanas intentando generar facturas
     */
    private ReconciliationResult processOrphanTransactions(List<GetnetTransaction> orphanTransactions, UUID tenantId) {
        ReconciliationResult result = new ReconciliationResult();
        
        for (GetnetTransaction orphanTx : orphanTransactions) {
            try {
                log.info("Procesando transacción huérfana: {}", orphanTx.getId());
                
                // Crear transacción local si no existe
                Transaction localTransaction = createOrUpdateLocalTransaction(orphanTx, tenantId);
                
                // Intentar generar factura
                if (localTransaction.getBillingStatus() == null || !"billed".equals(localTransaction.getBillingStatus())) {
                    try {
                        log.info("Generando factura automáticamente para huérfana {}", orphanTx.getId());
                        invoiceService.createFacturaInFacturante(localTransaction);
                        // Si llegó aquí, el servicio ya actualizó transaction (CAE, número, PDF)
                        localTransaction.setBillingStatus("billed");
                        localTransaction.setReconciled(true);
                        transactionRepository.save(localTransaction);
                        result.incrementProcessed();
                    } catch (IllegalArgumentException e) {
                        // Errores de validación: dejamos en pending y registramos error
                        log.error("Validación fallida al facturar huérfana {}: {}", orphanTx.getId(), e.getMessage());
                        localTransaction.setBillingStatus("error");
                        transactionRepository.save(localTransaction);
                        result.addError(orphanTx.getId(), e.getMessage());
                    } catch (Exception e) {
                        // Error técnico: registrar para posible reintento
                        log.error("Error técnico al facturar huérfana {}: {}", orphanTx.getId(), e.getMessage(), e);
                        localTransaction.setBillingStatus("error");
                        transactionRepository.save(localTransaction);
                        result.addError(orphanTx.getId(), e.getMessage());
                    }
                }
                
            } catch (Exception e) {
                log.error("Error procesando transacción huérfana {}: {}", orphanTx.getId(), e.getMessage(), e);
                result.addError(orphanTx.getId(), e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Crea o actualiza una transacción local basada en datos de Getnet
     */
    private Transaction createOrUpdateLocalTransaction(GetnetTransaction getnetTx, UUID tenantId) {
        // Buscar transacción existente por externalId
        Transaction existing = transactionRepository.findByExternalId(getnetTx.getId())
            .orElse(null);
        
        if (existing != null) {
            // Actualizar transacción existente
            existing.setStatus(TransactionStatus.PAID);
            existing.setAmount(java.math.BigDecimal.valueOf(getnetTx.getAmount()));
            existing.setUpdatedAt(java.time.OffsetDateTime.now());
            return transactionRepository.save(existing);
        } else {
            // Crear nueva transacción
            Transaction newTransaction = Transaction.builder()
                .externalId(getnetTx.getId())
                .status(TransactionStatus.PAID)
                .amount(java.math.BigDecimal.valueOf(getnetTx.getAmount()))
                .currency("ARS")
                .billingStatus("pending")
                .tenantId(tenantId)
                .reconciled(false)
                .build();
            
            return transactionRepository.save(newTransaction);
        }
    }
    
    // Clases de datos para el resultado
    public static class ReconciliationResult {
        private int processedCount = 0;
        private int errorCount = 0;
        private final Map<String, String> errors = new java.util.HashMap<>();
        
        public void incrementProcessed() { processedCount++; }
        public void addError(String transactionId, String error) { 
            errors.put(transactionId, error); 
            errorCount++; 
        }
        
        public boolean hasErrors() { return errorCount > 0; }
        public int getProcessedCount() { return processedCount; }
        public int getErrorCount() { return errorCount; }
        public Map<String, String> getErrors() { return errors; }
    }
    
    // Clase de datos para transacciones de Getnet
    public static class GetnetTransaction {
        private final String id;
        private final String status;
        private final Double amount;
        private final String timestamp;
        
        public GetnetTransaction(String id, String status, Double amount, String timestamp) {
            this.id = id;
            this.status = status;
            this.amount = amount;
            this.timestamp = timestamp;
        }
        
        public String getId() { return id; }
        public String getStatus() { return status; }
        public Double getAmount() { return amount; }
        public String getTimestamp() { return timestamp; }
    }
}