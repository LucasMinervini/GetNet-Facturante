package com.gf.connector.scheduler;

import com.gf.connector.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler para backup automático de configuración
 * Ejecuta backups regulares para proteger datos críticos
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupService backupService;
    
    @Value("${backup.enabled:true}")
    private boolean backupEnabled;

    /**
     * Backup diario - se ejecuta todos los días a las 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void dailyBackup() {
        if (!backupEnabled) {
            log.info("Backup automático deshabilitado");
            return;
        }
        
        log.info("Iniciando backup diario automático");
        
        try {
            BackupService.BackupResult result = backupService.performBackup();
            
            if (result.isSuccess()) {
                log.info("Backup diario completado exitosamente: {}", result.getMessage());
            } else {
                log.error("Backup diario falló: {}", result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error crítico en backup diario: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Backup semanal - se ejecuta los domingos a las 0:30 AM
     */
    @Scheduled(cron = "0 30 0 * * SUN")
    public void weeklyBackup() {
        if (!backupEnabled) {
            return;
        }
        
        log.info("Iniciando backup semanal automático");
        
        try {
            BackupService.BackupResult result = backupService.performBackup();
            
            if (result.isSuccess()) {
                log.info("Backup semanal completado exitosamente: {}", result.getMessage());
            } else {
                log.error("Backup semanal falló: {}", result.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error crítico en backup semanal: {}", e.getMessage(), e);
        }
    }
}
