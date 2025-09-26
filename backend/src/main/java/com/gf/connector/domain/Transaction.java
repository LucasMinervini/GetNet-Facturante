package com.gf.connector.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "external_id", unique = true)
    private String externalId; // ID de Getnet u otro

    private BigDecimal amount;
    private String currency;
    
    @Column(name = "status")
    @Convert(converter = com.gf.connector.domain.TransactionStatusConverter.class)
    private TransactionStatus status; // authorized|paid|refunded|failed
    
    private String customerDoc;

    private String invoiceNumber;
    private String cae;

    private OffsetDateTime capturedAt;
    private boolean reconciled;

    // Campos para manejo de reembolsos
    @Column(name = "refund_reason")
    private String refundReason; // Motivo del reembolso
    
    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt; // Fecha del reembolso
    
    @Column(name = "credit_note_number")
    private String creditNoteNumber; // Número de nota de crédito
    
    @Column(name = "credit_note_cae")
    private String creditNoteCae; // CAE de la nota de crédito
    
    @Column(name = "credit_note_status")
    private String creditNoteStatus; // pending|sent|error
    
    @Column(name = "credit_note_strategy")
    private String creditNoteStrategy; // automatic|manual|stub

    // Estado de facturación
    @Column(name = "billing_status")
    @Builder.Default
    private String billingStatus = "pending"; // pending|billed

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
