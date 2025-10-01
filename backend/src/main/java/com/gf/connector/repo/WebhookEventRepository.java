package com.gf.connector.repo;

import com.gf.connector.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    
    /**
     * Métodos para Dashboard y Reportes - contar eventos no procesados
     */
    long countByTenantIdAndCreatedAtBetweenAndProcessedFalse(UUID tenantId, OffsetDateTime start, OffsetDateTime end);
    long countByCreatedAtBetweenAndProcessedFalse(OffsetDateTime start, OffsetDateTime end);
}
