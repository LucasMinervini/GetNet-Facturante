package com.gf.connector.dto;

import com.gf.connector.domain.CreditNote;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CreditNoteDto {
    private UUID id;
    private UUID transactionId;
    private String creditNoteNumber;
    private String creditNoteCae;
    private String status;
    private String refundReason;
    private String strategy;
    private String pdfUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CreditNoteDto fromEntity(CreditNote creditNote) {
        CreditNoteDto dto = new CreditNoteDto();
        dto.setId(creditNote.getId());
        dto.setTransactionId(creditNote.getTransaction().getId());
        dto.setCreditNoteNumber(creditNote.getCreditNoteNumber());
        dto.setCreditNoteCae(creditNote.getCreditNoteCae());
        dto.setStatus(creditNote.getStatus());
        dto.setRefundReason(creditNote.getRefundReason());
        dto.setStrategy(creditNote.getStrategy());
        dto.setPdfUrl(creditNote.getPdfUrl());
        dto.setCreatedAt(creditNote.getCreatedAt());
        dto.setUpdatedAt(creditNote.getUpdatedAt());
        return dto;
    }
}
