package com.gf.connector.service;

import com.gf.connector.service.ReconciliationService.ReconciliationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para enviar notificaciones por email
 * Alertas para errores críticos del sistema
 * 
 * Nota: Este servicio solo se activa si spring.mail.host está configurado
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.mail.host")
public class NotificationService {

    private final JavaMailSender mailSender;
    
    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;
    
    @Value("${notification.email.admin-email:admin@getnet-facturante.com}")
    private String adminEmail;
    
    @Value("${notification.email.from:noreply@getnet-facturante.com}")
    private String fromEmail;
    
    @Value("${app.name:GetNet-Facturante}")
    private String appName;

    /**
     * Envía notificación de error en reconciliación
     */
    public void sendReconciliationErrorNotification(UUID tenantId, ReconciliationResult result) {
        if (!emailEnabled) {
            log.warn("Notificaciones por email deshabilitadas");
            return;
        }
        
        try {
            String subject = String.format("[%s] Error en Reconciliación - Tenant %s", 
                    appName, tenantId);
            
            StringBuilder body = new StringBuilder();
            body.append("Se detectaron errores durante el proceso de reconciliación:\n\n");
            body.append("Tenant ID: ").append(tenantId).append("\n");
            body.append("Fecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            body.append("Transacciones procesadas: ").append(result.getProcessedCount()).append("\n");
            body.append("Errores encontrados: ").append(result.getErrorCount()).append("\n\n");
            
            if (!result.getErrors().isEmpty()) {
                body.append("Detalles de errores:\n");
                for (Map.Entry<String, String> error : result.getErrors().entrySet()) {
                    body.append("- Transacción ").append(error.getKey()).append(": ").append(error.getValue()).append("\n");
                }
            }
            
            body.append("\nPor favor, revise los logs del sistema para más detalles.");
            
            sendEmail(adminEmail, subject, body.toString());
            
        } catch (Exception e) {
            log.error("Error enviando notificación de reconciliación: {}", e.getMessage(), e);
        }
    }

    /**
     * Envía notificación informativa cuando se detectan huérfanas sin errores
     */
    public void sendReconciliationInfoNotification(UUID tenantId, int processedCount) {
        if (!emailEnabled) {
            log.warn("Notificaciones por email deshabilitadas");
            return;
        }

        try {
            String subject = String.format("[%s] Reconciliación: %d huérfanas facturadas - Tenant %s",
                    appName, processedCount, tenantId);

            StringBuilder body = new StringBuilder();
            body.append("Reconciliación ejecutada sin errores.\n\n");
            body.append("Tenant ID: ").append(tenantId).append("\n");
            body.append("Fecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            body.append("Transacciones huérfanas facturadas: ").append(processedCount).append("\n");

            sendEmail(adminEmail, subject, body.toString());

        } catch (Exception e) {
            log.error("Error enviando notificación informativa de reconciliación: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía notificación de error en reconciliación (versión con excepción)
     */
    public void sendReconciliationErrorNotification(UUID tenantId, Exception error) {
        if (!emailEnabled) {
            return;
        }
        
        try {
            String subject = String.format("[%s] Error Crítico en Reconciliación - Tenant %s", 
                    appName, tenantId);
            
            StringBuilder body = new StringBuilder();
            body.append("Error crítico durante el proceso de reconciliación:\n\n");
            body.append("Tenant ID: ").append(tenantId).append("\n");
            body.append("Fecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            body.append("Error: ").append(error.getMessage()).append("\n");
            body.append("Tipo: ").append(error.getClass().getSimpleName()).append("\n\n");
            body.append("Stack trace:\n").append(getStackTrace(error));
            
            sendEmail(adminEmail, subject, body.toString());
            
        } catch (Exception e) {
            log.error("Error enviando notificación de error crítico: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía notificación de error al generar factura
     */
    public void sendInvoiceGenerationErrorNotification(UUID tenantId, String transactionId, Exception error) {
        if (!emailEnabled) {
            return;
        }
        
        try {
            String subject = String.format("[%s] Error Generando Factura - Tenant %s", 
                    appName, tenantId);
            
            StringBuilder body = new StringBuilder();
            body.append("Error al generar factura para transacción:\n\n");
            body.append("Tenant ID: ").append(tenantId).append("\n");
            body.append("Transacción ID: ").append(transactionId).append("\n");
            body.append("Fecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            body.append("Error: ").append(error.getMessage()).append("\n");
            body.append("Tipo: ").append(error.getClass().getSimpleName()).append("\n\n");
            body.append("Por favor, revise la configuración de Facturante y los logs del sistema.");
            
            sendEmail(adminEmail, subject, body.toString());
            
        } catch (Exception e) {
            log.error("Error enviando notificación de error de factura: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía notificación de error en webhook
     */
    public void sendWebhookErrorNotification(String provider, String eventId, Exception error) {
        if (!emailEnabled) {
            return;
        }
        
        try {
            String subject = String.format("[%s] Error en Webhook %s", appName, provider);
            
            StringBuilder body = new StringBuilder();
            body.append("Error procesando webhook:\n\n");
            body.append("Proveedor: ").append(provider).append("\n");
            body.append("Evento ID: ").append(eventId).append("\n");
            body.append("Fecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            body.append("Error: ").append(error.getMessage()).append("\n");
            body.append("Tipo: ").append(error.getClass().getSimpleName()).append("\n\n");
            body.append("Por favor, revise la configuración del webhook y los logs del sistema.");
            
            sendEmail(adminEmail, subject, body.toString());
            
        } catch (Exception e) {
            log.error("Error enviando notificación de error de webhook: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Envía notificación de backup completado
     */
    public void sendBackupCompletedNotification(UUID tenantId, boolean success, String details) {
        if (!emailEnabled) {
            return;
        }
        
        try {
            String subject = String.format("[%s] Backup %s - Tenant %s", 
                    appName, success ? "Exitoso" : "Fallido", tenantId);
            
            StringBuilder body = new StringBuilder();
            body.append("Resultado del backup automático:\n\n");
            body.append("Tenant ID: ").append(tenantId).append("\n");
            body.append("Fecha: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            body.append("Estado: ").append(success ? "Exitoso" : "Fallido").append("\n");
            body.append("Detalles: ").append(details).append("\n");
            
            sendEmail(adminEmail, subject, body.toString());
            
        } catch (Exception e) {
            log.error("Error enviando notificación de backup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Método privado para enviar email
     */
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            log.info("Email enviado exitosamente a: {}", to);
            
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", to, e.getMessage(), e);
        }
    }
    
    /**
     * Obtiene stack trace como string
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
