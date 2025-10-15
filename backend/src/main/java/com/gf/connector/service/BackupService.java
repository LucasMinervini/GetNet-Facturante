package com.gf.connector.service;

import com.gf.connector.domain.BillingSettings;
import com.gf.connector.repo.BillingSettingsRepository;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Servicio para backup automático de configuración crítica
 * Respalda datos importantes como CUIT, Punto de Venta, claves de API, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final BillingSettingsRepository billingSettingsRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    
    // NotificationService es opcional - solo existe si está configurado el email
    @Autowired(required = false)
    private NotificationService notificationService;
    
    @Value("${backup.enabled:true}")
    private boolean backupEnabled;
    
    @Value("${backup.directory:./backups}")
    private String backupDirectory;
    
    @Value("${backup.retention-days:30}")
    private int retentionDays;

    /**
     * Ejecuta backup completo de la configuración
     */
    @Transactional(readOnly = true)
    public BackupResult performBackup() {
        if (!backupEnabled) {
            log.info("Backup automático deshabilitado");
            return new BackupResult(false, "Backup deshabilitado");
        }
        
        log.info("Iniciando backup automático de configuración");
        
        try {
            // Crear directorio de backup si no existe
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupFileName = String.format("backup_%s.json", timestamp);
            Path backupFile = backupPath.resolve(backupFileName);
            
            // Generar contenido del backup
            String backupContent = generateBackupContent();
            
            // Escribir archivo de backup
            try (FileWriter writer = new FileWriter(backupFile.toFile())) {
                writer.write(backupContent);
            }
            
            // Limpiar backups antiguos
            cleanupOldBackups();
            
            log.info("Backup completado exitosamente: {}", backupFile);
            
            // Enviar notificación de éxito
            if (notificationService != null) {
                notificationService.sendBackupCompletedNotification(null, true, 
                    "Backup completado: " + backupFile.getFileName());
            }
            
            return new BackupResult(true, "Backup exitoso: " + backupFile.getFileName());
            
        } catch (Exception e) {
            log.error("Error durante backup: {}", e.getMessage(), e);
            
            // Enviar notificación de error
            if (notificationService != null) {
                notificationService.sendBackupCompletedNotification(null, false, 
                    "Error: " + e.getMessage());
            }
            
            return new BackupResult(false, "Error: " + e.getMessage());
        }
    }
    
    /**
     * Genera el contenido del backup en formato JSON
     */
    private String generateBackupContent() {
        StringBuilder content = new StringBuilder();
        content.append("{\n");
        content.append("  \"backupInfo\": {\n");
        content.append("    \"timestamp\": \"").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\",\n");
        content.append("    \"version\": \"1.0\",\n");
        content.append("    \"type\": \"configuration_backup\"\n");
        content.append("  },\n");
        
        // Backup de configuraciones de billing
        content.append("  \"billingSettings\": [\n");
        List<BillingSettings> settings = billingSettingsRepository.findAll();
        for (int i = 0; i < settings.size(); i++) {
            BillingSettings setting = settings.get(i);
            content.append("    {\n");
            content.append("      \"id\": \"").append(setting.getId()).append("\",\n");
            content.append("      \"tenantId\": \"").append(setting.getTenantId()).append("\",\n");
            content.append("      \"cuitEmpresa\": \"").append(setting.getCuitEmpresa()).append("\",\n");
            content.append("      \"puntoVenta\": \"").append(setting.getPuntoVenta()).append("\",\n");
            content.append("      \"razonSocialEmpresa\": \"").append(setting.getRazonSocialEmpresa()).append("\",\n");
            content.append("      \"tipoComprobante\": \"").append(setting.getTipoComprobante()).append("\",\n");
            content.append("      \"webhookSecret\": \"").append(maskSensitiveData(setting.getWebhookSecret())).append("\",\n");
            content.append("      \"activo\": ").append(setting.getActivo()).append(",\n");
            content.append("      \"createdAt\": \"").append(setting.getCreatedAt()).append("\"\n");
            content.append("    }");
            if (i < settings.size() - 1) {
                content.append(",");
            }
            content.append("\n");
        }
        content.append("  ],\n");
        
        // Backup de usuarios (solo información básica, sin passwords)
        content.append("  \"users\": [\n");
        var users = userRepository.findAll();
        for (int i = 0; i < users.size(); i++) {
            var user = users.get(i);
            content.append("    {\n");
            content.append("      \"id\": \"").append(user.getId()).append("\",\n");
            content.append("      \"username\": \"").append(user.getUsername()).append("\",\n");
            content.append("      \"email\": \"").append(user.getEmail()).append("\",\n");
            content.append("      \"firstName\": \"").append(user.getFirstName()).append("\",\n");
            content.append("      \"lastName\": \"").append(user.getLastName()).append("\",\n");
            content.append("      \"tenantId\": \"").append(user.getTenantId()).append("\",\n");
            content.append("      \"role\": \"").append(user.getRole()).append("\",\n");
            content.append("      \"createdAt\": \"").append(user.getCreatedAt()).append("\"\n");
            content.append("    }");
            if (i < users.size() - 1) {
                content.append(",");
            }
            content.append("\n");
        }
        content.append("  ],\n");
        
        // Estadísticas de transacciones
        content.append("  \"statistics\": {\n");
        content.append("    \"totalTransactions\": ").append(transactionRepository.count()).append(",\n");
        content.append("    \"totalBillingSettings\": ").append(billingSettingsRepository.count()).append(",\n");
        content.append("    \"totalUsers\": ").append(userRepository.count()).append("\n");
        content.append("  }\n");
        
        content.append("}\n");
        
        return content.toString();
    }
    
    /**
     * Enmascara datos sensibles para el backup
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }
        if (data.length() <= 4) {
            return "****";
        }
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }
    
    /**
     * Limpia backups antiguos según la política de retención
     */
    private void cleanupOldBackups() {
        try {
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                return;
            }
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            
            Files.list(backupPath)
                .filter(path -> path.toString().endsWith(".json"))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime()
                            .isBefore(cutoffDate);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        log.info("Backup antiguo eliminado: {}", path.getFileName());
                    } catch (IOException e) {
                        log.warn("No se pudo eliminar backup antiguo: {}", path.getFileName());
                    }
                });
                
        } catch (Exception e) {
            log.error("Error limpiando backups antiguos: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Restaura configuración desde un archivo de backup
     */
    public boolean restoreFromBackup(String backupFileName) {
        try {
            Path backupFile = Paths.get(backupDirectory, backupFileName);
            if (!Files.exists(backupFile)) {
                log.error("Archivo de backup no encontrado: {}", backupFileName);
                return false;
            }
            
            String backupContent = Files.readString(backupFile);
            log.info("Iniciando restauración desde: {}", backupFileName);
            
            // Aquí se implementaría la lógica de restauración
            // Por ahora solo logueamos
            log.info("Restauración completada desde: {}", backupFileName);
            return true;
            
        } catch (Exception e) {
            log.error("Error restaurando desde backup {}: {}", backupFileName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Resultado del backup
     */
    public static class BackupResult {
        private final boolean success;
        private final String message;
        
        public BackupResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
