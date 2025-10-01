package com.gf.connector.repo;

import com.gf.connector.domain.BillingSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingSettingsRepository extends JpaRepository<BillingSettings, UUID> {
    
    /**
     * Obtiene la configuración activa de facturación
     */
    Optional<BillingSettings> findByActivoTrueAndTenantId(java.util.UUID tenantId);
    
    /**
     * Verifica si existe una configuración activa
     */
    boolean existsByActivoTrueAndTenantId(java.util.UUID tenantId);
    
    /**
     * Desactiva todas las configuraciones (para activar solo una)
     */
    void deleteByActivoTrueAndTenantId(java.util.UUID tenantId);
    
    /**
     * Desactiva todas las configuraciones excepto la especificada
     */
    void deleteByActivoTrueAndIdNotAndTenantId(UUID id, java.util.UUID tenantId);

    /**
     * Busca configuración por secreto de webhook (ruteo multi-tenant)
     */
    Optional<BillingSettings> findByWebhookSecret(String webhookSecret);
    
    /**
     * Busca configuración activa por tenantId
     */
    Optional<BillingSettings> findByTenantIdAndActivoTrue(UUID tenantId);
    
    /**
     * Busca todas las configuraciones activas (para reconciliación)
     */
    java.util.List<BillingSettings> findByActivoTrue();
}
