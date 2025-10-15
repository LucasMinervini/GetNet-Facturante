package com.gf.connector.repo;

import com.gf.connector.domain.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    
    /**
     * Métodos para Dashboard y Reportes - contar eventos no procesados
     * Nota: WebhookEvent NO tiene tenantId, los webhooks son globales
     */
    long countByCreatedAtBetweenAndProcessedFalse(OffsetDateTime start, OffsetDateTime end);
    
    /**
     * Contar webhooks no procesados sin filtro de fecha
     */
    long countByProcessedFalse();
}
