package com.gf.connector.dto;

import com.gf.connector.domain.BillingSettings;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class BillingSettingsDto {
    private UUID id;
    private String cuitEmpresa;
    private String razonSocialEmpresa;
    private String puntoVenta;
    private String tipoComprobante;
    private BigDecimal ivaPorDefecto;
    private boolean facturarSoloPaid;
    private boolean requireBillingConfirmation;
    private boolean consumidorFinalPorDefecto;
    private String cuitConsumidorFinal;
    private String razonSocialConsumidorFinal;
    private String emailFacturacion;
    private boolean enviarComprobante;
    private boolean activo;
    private String descripcion;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    public static BillingSettingsDto fromEntity(BillingSettings settings) {
        BillingSettingsDto dto = new BillingSettingsDto();
        dto.setId(settings.getId());
        dto.setCuitEmpresa(settings.getCuitEmpresa());
        dto.setRazonSocialEmpresa(settings.getRazonSocialEmpresa());
        dto.setPuntoVenta(settings.getPuntoVenta());
        dto.setTipoComprobante(settings.getTipoComprobante());
        dto.setIvaPorDefecto(settings.getIvaPorDefecto());
        dto.setFacturarSoloPaid(Boolean.TRUE.equals(settings.getFacturarSoloPaid()));
        dto.setRequireBillingConfirmation(Boolean.TRUE.equals(settings.getRequireBillingConfirmation()));
        dto.setConsumidorFinalPorDefecto(Boolean.TRUE.equals(settings.getConsumidorFinalPorDefecto()));
        dto.setCuitConsumidorFinal(settings.getCuitConsumidorFinal());
        dto.setRazonSocialConsumidorFinal(settings.getRazonSocialConsumidorFinal());
        dto.setEmailFacturacion(settings.getEmailFacturacion());
        dto.setEnviarComprobante(Boolean.TRUE.equals(settings.getEnviarComprobante()));
        dto.setActivo(Boolean.TRUE.equals(settings.getActivo()));
        dto.setDescripcion(settings.getDescripcion());
        dto.setCreatedAt(settings.getCreatedAt());
        dto.setUpdatedAt(settings.getUpdatedAt());
        return dto;
    }
    
    public BillingSettings toEntity() {
        return BillingSettings.builder()
                .id(id)
                .cuitEmpresa(cuitEmpresa)
                .razonSocialEmpresa(razonSocialEmpresa)
                .puntoVenta(puntoVenta)
                .tipoComprobante(tipoComprobante)
                .ivaPorDefecto(ivaPorDefecto)
                .facturarSoloPaid(facturarSoloPaid)
                .requireBillingConfirmation(requireBillingConfirmation)
                .consumidorFinalPorDefecto(consumidorFinalPorDefecto)
                .cuitConsumidorFinal(cuitConsumidorFinal)
                .razonSocialConsumidorFinal(razonSocialConsumidorFinal)
                .emailFacturacion(emailFacturacion)
                .enviarComprobante(enviarComprobante)
                .activo(activo)
                .descripcion(descripcion)
                .build();
    }
}
