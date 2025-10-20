package com.gf.connector.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_logs", indexes = {
        @Index(name = "idx_rec_tenant_created", columnList = "tenant_id, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "start_date", nullable = false)
    private String startDate; // ISO yyyy-MM-dd

    @Column(name = "end_date", nullable = false)
    private String endDate; // ISO yyyy-MM-dd

    @Column(name = "processed_count")
    private int processedCount;

    @Column(name = "error_count")
    private int errorCount;

    @Column(name = "orphan_count")
    private int orphanCount;

    @Column(name = "reconciliation_rate")
    private Double reconciliationRate;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}


