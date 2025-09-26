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
    Optional<BillingSettings> findByActivoTrue();
    
    /**
     * Verifica si existe una configuración activa
     */
    boolean existsByActivoTrue();
    
    /**
     * Desactiva todas las configuraciones (para activar solo una)
     */
    void deleteByActivoTrue();
    
    /**
     * Desactiva todas las configuraciones excepto la especificada
     */
    void deleteByActivoTrueAndIdNot(UUID id);
}
