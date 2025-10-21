package com.gf.connector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.domain.BillingSettings;
import com.gf.connector.domain.CreditNote;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.facturante.model.CrearComprobanteResponse;
import com.gf.connector.facturante.service.FacturanteService;
import com.gf.connector.repo.CreditNoteRepository;
import com.gf.connector.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditNoteServiceTest {

    @Mock
    private CreditNoteRepository creditNoteRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private FacturanteService facturanteService;
    @Mock
    private BillingSettingsService billingSettingsService;

    @InjectMocks
    private CreditNoteService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        service = new CreditNoteService(creditNoteRepository, transactionRepository, 
                facturanteService, objectMapper, billingSettingsService);
    }

    @Test
    void processRefund_withPaidTransaction_createsCreditNote() {
        // Arrange
        Transaction transaction = createValidTransaction();
        String refundReason = "Customer requested refund";
        
        BillingSettings billingSettings = BillingSettings.builder()
                .creditNoteStrategy("stub")
                .build();
        
        when(creditNoteRepository.findByTransactionId(transaction.getId())).thenReturn(Optional.empty());
        when(billingSettingsService.getActiveSettings(transaction.getTenantId()))
                .thenReturn(Optional.of(billingSettings));
        when(creditNoteRepository.save(any(CreditNote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreditNote result = service.processRefund(transaction, refundReason);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTransaction()).isEqualTo(transaction);
        assertThat(result.getRefundReason()).isEqualTo(refundReason);
        assertThat(result.getStrategy()).isEqualTo("stub");
        verify(creditNoteRepository, atLeastOnce()).save(any(CreditNote.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void processRefund_withNonPaidTransaction_throwsException() {
        // Arrange
        Transaction transaction = createValidTransaction();
        transaction.setStatus(TransactionStatus.AUTHORIZED); // No está pagada
        String refundReason = "Customer requested refund";

        // Act & Assert
        assertThatThrownBy(() -> service.processRefund(transaction, refundReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo se pueden reembolsar transacciones pagadas");
    }

    @Test
    void processRefund_withExistingCreditNote_returnsExisting() {
        // Arrange
        Transaction transaction = createValidTransaction();
        String refundReason = "Customer requested refund";
        CreditNote existingCreditNote = CreditNote.builder()
                .id(UUID.randomUUID())
                .transaction(transaction)
                .build();
        
        when(creditNoteRepository.findByTransactionId(transaction.getId()))
                .thenReturn(Optional.of(existingCreditNote));

        // Act
        CreditNote result = service.processRefund(transaction, refundReason);

        // Assert
        assertThat(result).isEqualTo(existingCreditNote);
        verify(creditNoteRepository, never()).save(any(CreditNote.class));
    }

    @Test
    void findByTransactionId_withExistingTransaction_returnsCreditNote() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        CreditNote creditNote = CreditNote.builder()
                .id(UUID.randomUUID())
                .build();
        
        when(creditNoteRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(creditNote));

        // Act
        Optional<CreditNote> result = service.findByTransactionId(transactionId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(creditNote);
    }

    @Test
    void findByTransactionId_withNonExistingTransaction_returnsEmpty() {
        // Arrange
        UUID transactionId = UUID.randomUUID();
        when(creditNoteRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

        // Act
        Optional<CreditNote> result = service.findByTransactionId(transactionId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findById_withExistingId_returnsCreditNote() {
        // Arrange
        UUID creditNoteId = UUID.randomUUID();
        CreditNote creditNote = CreditNote.builder()
                .id(creditNoteId)
                .build();
        
        when(creditNoteRepository.findById(creditNoteId)).thenReturn(Optional.of(creditNote));

        // Act
        Optional<CreditNote> result = service.findById(creditNoteId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(creditNote);
    }

    @Test
    void findById_withNonExistingId_returnsEmpty() {
        // Arrange
        UUID creditNoteId = UUID.randomUUID();
        when(creditNoteRepository.findById(creditNoteId)).thenReturn(Optional.empty());

        // Act
        Optional<CreditNote> result = service.findById(creditNoteId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void findByCreditNoteNumber_withExistingNumber_returnsCreditNote() {
        // Arrange
        String creditNoteNumber = "NC-12345";
        CreditNote creditNote = CreditNote.builder()
                .id(UUID.randomUUID())
                .creditNoteNumber(creditNoteNumber)
                .build();
        
        when(creditNoteRepository.findByCreditNoteNumber(creditNoteNumber)).thenReturn(Optional.of(creditNote));

        // Act
        Optional<CreditNote> result = service.findByCreditNoteNumber(creditNoteNumber);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(creditNote);
    }

    @Test
    void processManualCreditNote_withPendingCreditNote_processesIt() {
        // Arrange
        UUID creditNoteId = UUID.randomUUID();
        Transaction transaction = createValidTransaction();
        CreditNote creditNote = CreditNote.builder()
                .id(creditNoteId)
                .transaction(transaction)
                .status("pending")
                .build();
        
        CrearComprobanteResponse response = new CrearComprobanteResponse();
        response.setExitoso(true);
        response.setCae("CAE12345");
        response.setNumeroComprobante("12345");
        
        when(creditNoteRepository.findById(creditNoteId)).thenReturn(Optional.of(creditNote));
        when(facturanteService.crearNotaCredito(transaction)).thenReturn(response);
        when(creditNoteRepository.save(any(CreditNote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreditNote result = service.processManualCreditNote(creditNoteId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("sent");
        assertThat(result.getCreditNoteCae()).isEqualTo("CAE12345");
        verify(creditNoteRepository, atLeastOnce()).save(any(CreditNote.class));
    }

    @Test
    void processManualCreditNote_withNonPendingCreditNote_throwsException() {
        // Arrange
        UUID creditNoteId = UUID.randomUUID();
        Transaction transaction = createValidTransaction();
        CreditNote creditNote = CreditNote.builder()
                .id(creditNoteId)
                .transaction(transaction)
                .status("sent") // No está pendiente
                .build();
        
        when(creditNoteRepository.findById(creditNoteId)).thenReturn(Optional.of(creditNote));

        // Act & Assert
        assertThatThrownBy(() -> service.processManualCreditNote(creditNoteId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo se pueden procesar notas de crédito pendientes");
    }

    private Transaction createValidTransaction() {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("TXN-123")
                .amount(new BigDecimal("100.50"))
                .status(TransactionStatus.PAID)
                .customerDoc("20123456789")
                .currency("ARS")
                .tenantId(UUID.randomUUID())
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
