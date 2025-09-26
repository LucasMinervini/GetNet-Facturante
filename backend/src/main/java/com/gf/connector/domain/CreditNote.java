package com.gf.connector.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_notes")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CreditNote {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(name = "credit_note_number")
    private String creditNoteNumber; // Número de nota de crédito

    @Column(name = "credit_note_cae")
    private String creditNoteCae; // CAE de la nota de crédito

    private String status; // pending|sent|error|stub

    @Column(name = "refund_reason")
    private String refundReason; // Motivo del reembolso

    @Column(name = "strategy")
    private String strategy; // automatic|manual|stub

    @Column(columnDefinition = "TEXT")
    private String requestJson;

    @Column(columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "pdf_url")
    private String pdfUrl;

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
}
