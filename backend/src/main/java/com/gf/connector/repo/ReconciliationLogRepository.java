package com.gf.connector.repo;

import com.gf.connector.domain.ReconciliationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReconciliationLogRepository extends JpaRepository<ReconciliationLog, UUID> {
    Page<ReconciliationLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}


