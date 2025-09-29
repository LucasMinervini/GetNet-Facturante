package com.gf.connector.dto;

import com.gf.connector.domain.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionDto(
    UUID id,
    String externalId,
    BigDecimal amount,
    String currency,
    TransactionStatus status,
    String customerDoc,
    String invoiceNumber,
    String cae,
    LocalDateTime capturedAt,
    boolean reconciled,
    // Campos de reembolso
    String refundReason,
    LocalDateTime refundedAt,
    String creditNoteNumber,
    String creditNoteCae,
    String creditNoteStatus,
    String creditNoteStrategy,
    String billingStatus,
    String invoicePdfUrl,
    String creditNotePdfUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TransactionDto fromEntity(com.gf.connector.domain.Transaction transaction) {
        return new TransactionDto(
            transaction.getId(),
            transaction.getExternalId(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getStatus(),
            transaction.getCustomerDoc(),
            transaction.getInvoiceNumber(),
            transaction.getCae(),
            transaction.getCapturedAt() != null ? transaction.getCapturedAt().toLocalDateTime() : null,
            transaction.isReconciled(),
            // Campos de reembolso
            transaction.getRefundReason(),
            transaction.getRefundedAt() != null ? transaction.getRefundedAt().toLocalDateTime() : null,
            transaction.getCreditNoteNumber(),
            transaction.getCreditNoteCae(),
            transaction.getCreditNoteStatus(),
            transaction.getCreditNoteStrategy(),
            transaction.getBillingStatus(),
            transaction.getInvoicePdfUrl(),
            transaction.getCreditNotePdfUrl(),
            transaction.getCreatedAt() != null ? transaction.getCreatedAt().toLocalDateTime() : null,
            transaction.getUpdatedAt() != null ? transaction.getUpdatedAt().toLocalDateTime() : null
        );
    }
}