package com.gf.connector.scheduler;

import com.gf.connector.domain.BillingSettings;
import com.gf.connector.repo.BillingSettingsRepository;
import com.gf.connector.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Scheduler para ejecutar jobs de reconciliación automática
 * Garantiza que ninguna transacción PAID de Getnet se quede sin facturar
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final ReconciliationService reconciliationService;
    private final BillingSettingsRepository billingSettingsRepository;
    
    @Value("${reconciliation.enabled:true}")
    private boolean reconciliationEnabled;
    
    @Value("${reconciliation.days-to-check:7}")
    private int daysToCheck;

    /**
     * Job de reconciliación diaria - se ejecuta todos los días a las 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReconciliation() {
        if (!reconciliationEnabled) {
            log.info("Reconciliación automática deshabilitada");
            return;
        }
        
        log.info("Iniciando job de reconciliación diaria");
        
        try {
            // Obtener todos los tenants activos
            List<BillingSettings> activeSettings = billingSettingsRepository.findByActivoTrue();
            
            if (activeSettings.isEmpty()) {
                log.warn("No hay configuraciones activas para reconciliación");
                return;
            }
            
            LocalDate endDate = LocalDate.now().minusDays(1); // Ayer
            LocalDate startDate = endDate.minusDays(daysToCheck - 1);
            
            log.info("Reconciliando transacciones desde {} hasta {}", startDate, endDate);
            
            int totalProcessed = 0;
            int totalErrors = 0;
            
            // Procesar cada tenant
            for (BillingSettings settings : activeSettings) {
                UUID tenantId = settings.getTenantId();
                
                try {
                    log.info("Reconciliando tenant: {}", tenantId);
                    
                    ReconciliationService.ReconciliationResult result = 
                        reconciliationService.performReconciliation(tenantId, startDate, endDate);
                    
                    totalProcessed += result.getProcessedCount();
                    totalErrors += result.getErrorCount();
                    
                    log.info("Tenant {} reconciliado: {} procesadas, {} errores", 
                            tenantId, result.getProcessedCount(), result.getErrorCount());
                    
                } catch (Exception e) {
                    log.error("Error reconciliando tenant {}: {}", tenantId, e.getMessage(), e);
                    totalErrors++;
                }
            }
            
            log.info("Job de reconciliación completado: {} transacciones procesadas, {} errores", 
                    totalProcessed, totalErrors);
            
        } catch (Exception e) {
            log.error("Error crítico en job de reconciliación: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Job de reconciliación semanal - se ejecuta los domingos a las 3:00 AM
     * Revisa los últimos 30 días para capturar cualquier transacción perdida
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void weeklyDeepReconciliation() {
        if (!reconciliationEnabled) {
            return;
        }
        
        log.info("Iniciando job de reconciliación semanal profunda");
        
        try {
            List<BillingSettings> activeSettings = billingSettingsRepository.findByActivoTrue();
            
            if (activeSettings.isEmpty()) {
                return;
            }
            
            LocalDate endDate = LocalDate.now().minusDays(1);
            LocalDate startDate = endDate.minusDays(30); // Últimos 30 días
            
            log.info("Reconciliación profunda desde {} hasta {}", startDate, endDate);
            
            for (BillingSettings settings : activeSettings) {
                UUID tenantId = settings.getTenantId();
                
                try {
                    ReconciliationService.ReconciliationResult result = 
                        reconciliationService.performReconciliation(tenantId, startDate, endDate);
                    
                    log.info("Reconciliación profunda tenant {}: {} procesadas, {} errores", 
                            tenantId, result.getProcessedCount(), result.getErrorCount());
                    
                } catch (Exception e) {
                    log.error("Error en reconciliación profunda tenant {}: {}", tenantId, e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error crítico en reconciliación semanal: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Job de limpieza de datos antiguos - se ejecuta mensualmente
     */
    @Scheduled(cron = "0 0 4 1 * ?")
    public void monthlyCleanup() {
        log.info("Iniciando limpieza mensual de datos antiguos");
        
        try {
            // Aquí se implementaría la limpieza de:
            // - Logs antiguos
            // - Datos de auditoría expirados
            // - Cache de rate limiting
            // - Archivos temporales
            
            log.info("Limpieza mensual completada");
            
        } catch (Exception e) {
            log.error("Error en limpieza mensual: {}", e.getMessage(), e);
        }
    }
}
