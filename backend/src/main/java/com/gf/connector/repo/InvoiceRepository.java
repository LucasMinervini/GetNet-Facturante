package com.gf.connector.repo;

import com.gf.connector.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    
    /**
     * Busca una factura por el ID de la transacción asociada
     */
    Optional<Invoice> findByTransactionIdAndTenantId(UUID transactionId, java.util.UUID tenantId);
    
    /**
     * Métodos para Dashboard y Reportes
     */
    long countByTenantIdAndCreatedAtBetween(UUID tenantId, OffsetDateTime start, OffsetDateTime end);
    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);
}
