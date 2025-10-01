package com.gf.connector.service;

import com.gf.connector.domain.BillingSettings;
import com.gf.connector.dto.BillingSettingsDto;
import com.gf.connector.repo.BillingSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingSettingsService {
    
    private final BillingSettingsRepository billingSettingsRepository;
    private static final java.util.UUID DEFAULT_TEST_TENANT = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    
    /**
     * Obtiene la configuración activa de facturación
     */
    public Optional<BillingSettings> getActiveSettings(java.util.UUID tenantId) {
        return billingSettingsRepository.findByActivoTrueAndTenantId(tenantId);
    }

    // Compatibilidad tests
    public Optional<BillingSettings> getActiveSettings() {
        return getActiveSettings(DEFAULT_TEST_TENANT);
    }
    
    /**
     * Obtiene la configuración activa como DTO
     */
    public Optional<BillingSettingsDto> getActiveSettingsDto(java.util.UUID tenantId) {
        return getActiveSettings(tenantId).map(BillingSettingsDto::fromEntity);
    }

    // Compatibilidad tests
    public Optional<BillingSettingsDto> getActiveSettingsDto() {
        return getActiveSettingsDto(DEFAULT_TEST_TENANT);
    }
    
    /**
     * Obtiene todas las configuraciones
     */
    public List<BillingSettingsDto> getAllSettings(java.util.UUID tenantId) {
        return billingSettingsRepository.findAll().stream()
                .filter(s -> tenantId.equals(s.getTenantId()))
                .map(BillingSettingsDto::fromEntity)
                .toList();
    }

    // Compatibilidad tests
    public List<BillingSettingsDto> getAllSettings() {
        return getAllSettings(DEFAULT_TEST_TENANT);
    }
    
    /**
     * Obtiene una configuración por ID
     */
    public Optional<BillingSettingsDto> getSettingsById(UUID id, java.util.UUID tenantId) {
        return billingSettingsRepository.findById(id)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .map(BillingSettingsDto::fromEntity);
    }

    // Compatibilidad tests
    public Optional<BillingSettingsDto> getSettingsById(UUID id) {
        return getSettingsById(id, DEFAULT_TEST_TENANT);
    }
    
    /**
     * Crea una nueva configuración
     */
    @Transactional
    public BillingSettingsDto createSettings(BillingSettingsDto dto, java.util.UUID tenantId) {
        log.info("Creando nueva configuración de facturación: {}", dto.getDescripcion());
        
        // Si se marca como activa, desactivar las demás
        if (dto.isActivo()) {
            billingSettingsRepository.deleteByActivoTrueAndTenantId(tenantId);
        }
        
        BillingSettings settings = dto.toEntity();
        settings.setTenantId(tenantId);
        
        // Asegurar que los campos obligatorios nunca sean null
        if (settings.getIvaPorDefecto() == null) {
            settings.setIvaPorDefecto(new BigDecimal("21.00"));
        }
        if (settings.getTipoComprobante() == null || settings.getTipoComprobante().trim().isEmpty()) {
            settings.setTipoComprobante("FB");
        }
        if (settings.getPuntoVenta() == null || settings.getPuntoVenta().trim().isEmpty()) {
            settings.setPuntoVenta("0001");
        }
        
        settings = billingSettingsRepository.save(settings);
        
        log.info("Configuración creada con ID: {}", settings.getId());
        return BillingSettingsDto.fromEntity(settings);
    }

    // Compatibilidad tests
    @Transactional
    public BillingSettingsDto createSettings(BillingSettingsDto dto) {
        return createSettings(dto, DEFAULT_TEST_TENANT);
    }
    
    /**
     * Actualiza una configuración existente
     */
    @Transactional
    public BillingSettingsDto updateSettings(UUID id, BillingSettingsDto dto, java.util.UUID tenantId) {
        log.info("Actualizando configuración de facturación: {}", id);
        
        BillingSettings existingSettings = billingSettingsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada: " + id));
        if (!tenantId.equals(existingSettings.getTenantId())) {
            throw new IllegalArgumentException("Configuración no pertenece al tenant");
        }
        
        // Si se marca como activa, desactivar las demás (excluyendo la actual)
        if (dto.isActivo()) {
            billingSettingsRepository.deleteByActivoTrueAndIdNotAndTenantId(id, tenantId);
        }
        
        // Actualizar campos
        existingSettings.setCuitEmpresa(dto.getCuitEmpresa());
        existingSettings.setRazonSocialEmpresa(dto.getRazonSocialEmpresa());
        
        // Asegurar que los campos obligatorios nunca sean null
        BigDecimal ivaPorDefecto = dto.getIvaPorDefecto();
        if (ivaPorDefecto == null) {
            ivaPorDefecto = new BigDecimal("21.00");
        }
        existingSettings.setIvaPorDefecto(ivaPorDefecto);
        
        String tipoComprobante = dto.getTipoComprobante();
        if (tipoComprobante == null || tipoComprobante.trim().isEmpty()) {
            tipoComprobante = "FB";
        }
        existingSettings.setTipoComprobante(tipoComprobante);
        
        String puntoVenta = dto.getPuntoVenta();
        if (puntoVenta == null || puntoVenta.trim().isEmpty()) {
            puntoVenta = "0001";
        }
        existingSettings.setPuntoVenta(puntoVenta);
        existingSettings.setFacturarSoloPaid(dto.isFacturarSoloPaid());
        existingSettings.setConsumidorFinalPorDefecto(dto.isConsumidorFinalPorDefecto());
        existingSettings.setCuitConsumidorFinal(dto.getCuitConsumidorFinal());
        existingSettings.setRazonSocialConsumidorFinal(dto.getRazonSocialConsumidorFinal());
        existingSettings.setEmailFacturacion(dto.getEmailFacturacion());
        existingSettings.setEnviarComprobante(dto.isEnviarComprobante());
        existingSettings.setActivo(dto.isActivo());
        existingSettings.setDescripcion(dto.getDescripcion());
        
        existingSettings = billingSettingsRepository.save(existingSettings);
        
        log.info("Configuración actualizada: {}", existingSettings.getId());
        return BillingSettingsDto.fromEntity(existingSettings);
    }

    // Compatibilidad tests
    @Transactional
    public BillingSettingsDto updateSettings(UUID id, BillingSettingsDto dto) {
        return updateSettings(id, dto, DEFAULT_TEST_TENANT);
    }
    
    /**
     * Activa una configuración específica
     */
    @Transactional
    public BillingSettingsDto activateSettings(UUID id, java.util.UUID tenantId) {
        log.info("Activando configuración: {}", id);
        
        // Desactivar todas las configuraciones excepto la especificada
        billingSettingsRepository.deleteByActivoTrueAndIdNotAndTenantId(id, tenantId);
        
        // Activar la especificada
        BillingSettings settings = billingSettingsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada: " + id));
        if (!tenantId.equals(settings.getTenantId())) {
            throw new IllegalArgumentException("Configuración no pertenece al tenant");
        }
        
        settings.setActivo(true);
        settings = billingSettingsRepository.save(settings);
        
        log.info("Configuración activada: {}", settings.getId());
        return BillingSettingsDto.fromEntity(settings);
    }

    // Compatibilidad tests
    @Transactional
    public BillingSettingsDto activateSettings(UUID id) {
        return activateSettings(id, DEFAULT_TEST_TENANT);
    }
    
    /**
     * Elimina una configuración
     */
    @Transactional
    public void deleteSettings(UUID id, java.util.UUID tenantId) {
        log.info("Eliminando configuración: {}", id);
        
        BillingSettings settings = billingSettingsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada: " + id));
        if (!tenantId.equals(settings.getTenantId())) {
            throw new IllegalArgumentException("Configuración no pertenece al tenant");
        }
        
        // No permitir eliminar la configuración activa si es la única
        if (Boolean.TRUE.equals(settings.getActivo()) && billingSettingsRepository.existsByActivoTrueAndTenantId(tenantId) && billingSettingsRepository.count() == 1) {
            throw new IllegalStateException("No se puede eliminar la única configuración activa");
        }
        
        billingSettingsRepository.delete(settings);
        log.info("Configuración eliminada: {}", id);
    }

    // Compatibilidad tests
    @Transactional
    public void deleteSettings(UUID id) {
        deleteSettings(id, DEFAULT_TEST_TENANT);
    }
    
    /**
     * Crea una configuración por defecto si no existe ninguna
     */
    @Transactional
    public void createDefaultSettingsIfNotExists(java.util.UUID tenantId) {
        if (!billingSettingsRepository.existsByActivoTrueAndTenantId(tenantId)) {
            log.info("Creando configuración por defecto");
            
            BillingSettings defaultSettings = BillingSettings.builder()
                    .cuitEmpresa("")
                    .razonSocialEmpresa("")
                    .puntoVenta("0001")
                    .tipoComprobante("FB")
                    .ivaPorDefecto(new java.math.BigDecimal("21.00"))
                    .facturarSoloPaid(true)
                    .consumidorFinalPorDefecto(true)
                    .cuitConsumidorFinal("00000000000")
                    .razonSocialConsumidorFinal("Consumidor Final")
                    .emailFacturacion("")
                    .enviarComprobante(true)
                    .activo(true)
                    .descripcion("Configuración por defecto")
                    .build();
            defaultSettings.setTenantId(tenantId);
            
            billingSettingsRepository.save(defaultSettings);
            log.info("Configuración por defecto creada");
        }
    }

    // Compatibilidad tests
    @Transactional
    public void createDefaultSettingsIfNotExists() {
        createDefaultSettingsIfNotExists(DEFAULT_TEST_TENANT);
    }
}
