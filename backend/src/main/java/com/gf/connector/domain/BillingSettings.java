package com.gf.connector.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "billing_settings", indexes = {
    @Index(name = "idx_billing_settings_tenant", columnList = "tenant_id")
})
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BillingSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Datos de la empresa
    @Column(name = "cuit_empresa", length = 11)
    private String cuitEmpresa;
    
    @Column(name = "razon_social_empresa", length = 200)
    private String razonSocialEmpresa;
    
    @Column(name = "punto_venta", length = 4)
    private String puntoVenta;
    
    @Column(name = "tipo_comprobante", length = 2)
    private String tipoComprobante; // FA, FB, FC
    
    // Configuración de IVA
    @Builder.Default
    @Column(name = "iva_por_defecto", precision = 5, scale = 2)
    private BigDecimal ivaPorDefecto = new BigDecimal("21.00");
    
    // Reglas de facturación
    @Builder.Default
    @Column(name = "facturar_solo_paid", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean facturarSoloPaid = true;
    
    @Builder.Default
    @Column(name = "require_billing_confirmation", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean requireBillingConfirmation = false;
    
    @Builder.Default
    @Column(name = "consumidor_final_por_defecto", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean consumidorFinalPorDefecto = true;
    
    @Builder.Default
    @Column(name = "cuit_consumidor_final", length = 11)
    private String cuitConsumidorFinal = "00000000000";
    
    @Builder.Default
    @Column(name = "razon_social_consumidor_final", length = 200)
    private String razonSocialConsumidorFinal = "Consumidor Final";
    
    // Configuración adicional
    @Column(name = "email_facturacion", length = 200)
    private String emailFacturacion;
    
    @Builder.Default
    @Column(name = "enviar_comprobante", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean enviarComprobante = true;
    
    // Configuración de notas de crédito
    @Builder.Default
    @Column(name = "credit_note_strategy", length = 20)
    private String creditNoteStrategy = "stub"; // automatic|manual|stub
    
    @Builder.Default
    @Column(name = "activo", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activo = true;
    
    @Column(name = "descripcion", length = 500)
    private String descripcion;
    
    // Secreto único por tenant para identificar webhooks del proveedor
    @Column(name = "webhook_secret", length = 128)
    private String webhookSecret;

    @Column(name = "tenant_id", nullable = false)
    private java.util.UUID tenantId;
    
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    
    private OffsetDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
    
    // Métodos de utilidad
    public boolean isCuitValido(String cuit) {
        if (cuit == null || cuit.length() != 11) {
            return false;
        }
        
        // Validación básica de dígito verificador
        try {
            int[] multipliers = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
            int sum = 0;
            
            for (int i = 0; i < 10; i++) {
                sum += Character.getNumericValue(cuit.charAt(i)) * multipliers[i];
            }
            
            int remainder = sum % 11;
            int checkDigit = remainder < 2 ? remainder : 11 - remainder;
            
            return checkDigit == Character.getNumericValue(cuit.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean esConsumidorFinal(String cuit) {
        return cuit == null || cuit.trim().isEmpty() || 
               cuit.equals("00000000000") || 
               cuit.equals(cuitConsumidorFinal);
    }
    
    public String getTipoDocumentoCliente(String cuit) {
        if (esConsumidorFinal(cuit)) {
            return "99"; // Consumidor Final
        } else if (isCuitValido(cuit)) {
            return "80"; // CUIT
        } else {
            return "96"; // DNI
        }
    }
}
