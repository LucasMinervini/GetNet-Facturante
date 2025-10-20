package com.gf.connector.dto;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TransactionDtoTest {

    @Test
    void fromEntity_mapsAllFields_andConvertsDatesSafely() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        Transaction tx = Transaction.builder()
            .id(id)
            .externalId("EXT-123")
            .amount(new BigDecimal("123.45"))
            .currency("ARS")
            .status(TransactionStatus.PAID)
            .customerDoc("20123456789")
            .invoiceNumber("0001-00000042")
            .cae("CAE-XYZ")
            .capturedAt(now)
            .reconciled(true)
            .refundReason("Devolución parcial")
            .refundedAt(now)
            .creditNoteNumber("NC-0001")
            .creditNoteCae("CAE-NC")
            .creditNoteStatus("sent")
            .creditNoteStrategy("automatic")
            .billingStatus("billed")
            .invoicePdfUrl("http://pdf/invoice.pdf")
            .creditNotePdfUrl("http://pdf/credit-note.pdf")
            .createdAt(now)
            .updatedAt(now)
            .tenantId(UUID.randomUUID())
            .build();

        TransactionDto dto = TransactionDto.fromEntity(tx);

        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals("EXT-123", dto.externalId());
        assertEquals(new BigDecimal("123.45"), dto.amount());
        assertEquals("ARS", dto.currency());
        assertEquals(TransactionStatus.PAID, dto.status());
        assertEquals("20123456789", dto.customerDoc());
        assertEquals("0001-00000042", dto.invoiceNumber());
        assertEquals("CAE-XYZ", dto.cae());
        assertNotNull(dto.capturedAt());
        assertTrue(dto.reconciled());
        assertEquals("Devolución parcial", dto.refundReason());
        assertNotNull(dto.refundedAt());
        assertEquals("NC-0001", dto.creditNoteNumber());
        assertEquals("CAE-NC", dto.creditNoteCae());
        assertEquals("sent", dto.creditNoteStatus());
        assertEquals("automatic", dto.creditNoteStrategy());
        assertEquals("billed", dto.billingStatus());
        assertEquals("http://pdf/invoice.pdf", dto.invoicePdfUrl());
        assertEquals("http://pdf/credit-note.pdf", dto.creditNotePdfUrl());
        assertNotNull(dto.createdAt());
        assertNotNull(dto.updatedAt());
    }

    @Test
    void fromEntity_handlesNullDates_withoutThrowing() {
        Transaction tx = Transaction.builder()
            .id(UUID.randomUUID())
            .externalId("E")
            .amount(new BigDecimal("1"))
            .currency("ARS")
            .status(TransactionStatus.AUTHORIZED)
            .reconciled(false)
            .build();

        TransactionDto dto = TransactionDto.fromEntity(tx);

        assertNotNull(dto);
        assertNull(dto.capturedAt());
        assertNull(dto.refundedAt());
        assertNull(dto.createdAt());
        assertNull(dto.updatedAt());
    }
}


