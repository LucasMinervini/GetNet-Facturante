package com.gf.connector.controllers;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.dto.TransactionDto;
import com.gf.connector.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionsControllerTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionsController controller;

    private Transaction tx;
    private Page<Transaction> emptyPage;
    private Page<Transaction> singlePage;

    @BeforeEach
    void setUp() {
        tx = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("EXT-1")
                .amount(new BigDecimal("100.00"))
                .currency("ARS")
                .status(TransactionStatus.PAID)
                .customerDoc("12345678")
                .capturedAt(OffsetDateTime.now())
                .build();
        emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20, Sort.by("createdAt").descending()), 0);
        singlePage = new PageImpl<>(List.of(tx), PageRequest.of(0, 20, Sort.by("createdAt").descending()), 1);
    }

    @Test
    void list_searchText() {
        when(transactionRepository.findBySearchText(eq("abc"), any(Pageable.class))).thenReturn(singlePage);
        Page<TransactionDto> page = controller.list(0, 20, "createdAt", "desc", null, null, null, null, null, "abc");
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findBySearchText(eq("abc"), any(Pageable.class));
    }

    @Test
    void list_noFilters() {
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(singlePage);
        Page<TransactionDto> page = controller.list(0, 20, "createdAt", "desc", null, null, null, null, null, null);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void list_statusOnly() {
        when(transactionRepository.findByStatus(eq(TransactionStatus.PAID), any(Pageable.class))).thenReturn(singlePage);
        Page<TransactionDto> page = controller.list(0, 20, "createdAt", "desc", TransactionStatus.PAID, null, null, null, null, null);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findByStatus(eq(TransactionStatus.PAID), any(Pageable.class));
    }

    @Test
    void list_amountRangeOnly() {
        when(transactionRepository.findByAmountBetween(any(), any(), any(Pageable.class))).thenReturn(singlePage);
        Page<TransactionDto> page = controller.list(0, 20, "createdAt", "desc", null, new BigDecimal("10"), new BigDecimal("200"), null, null, null);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findByAmountBetween(eq(new BigDecimal("10")), eq(new BigDecimal("200")), any(Pageable.class));
    }

    @Test
    void list_dateRangeOnly() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(1);
        OffsetDateTime end = OffsetDateTime.now();
        when(transactionRepository.findByCreatedAtBetween(eq(start), eq(end), any(Pageable.class))).thenReturn(singlePage);
        Page<TransactionDto> page = controller.list(0, 20, "createdAt", "desc", null, null, null, start, end, null);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findByCreatedAtBetween(eq(start), eq(end), any(Pageable.class));
    }

    @Test
    void list_statusAndDates_onlyBranch() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(3);
        OffsetDateTime end = OffsetDateTime.now();
        when(transactionRepository.findByStatusAndCreatedAtBetween(eq(TransactionStatus.AUTHORIZED), eq(start), eq(end), any(Pageable.class)))
                .thenReturn(singlePage);
        Page<TransactionDto> page = controller.list(0, 20, "createdAt", "asc", TransactionStatus.AUTHORIZED, null, null, start, end, null);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findByStatusAndCreatedAtBetween(eq(TransactionStatus.AUTHORIZED), eq(start), eq(end), any(Pageable.class));
    }

    @Test
    void list_statusAndAmounts_onlyBranch() {
        when(transactionRepository.findByStatusAndAmountBetween(eq(TransactionStatus.PAID), eq(new BigDecimal("10")), eq(new BigDecimal("20")), any(Pageable.class)))
                .thenReturn(singlePage);
        Page<TransactionDto> page = controller.list(0, 10, "createdAt", "desc", TransactionStatus.PAID, new BigDecimal("10"), new BigDecimal("20"), null, null, null);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findByStatusAndAmountBetween(eq(TransactionStatus.PAID), eq(new BigDecimal("10")), eq(new BigDecimal("20")), any(Pageable.class));
    }

    @Test
    void list_complexFallback_findAllWhenMixedFilters() {
        // status + only minAmount triggers fallback to findAll
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(singlePage);
        Page<TransactionDto> page = controller.list(1, 5, "createdAt", "asc", TransactionStatus.PAID, new BigDecimal("5"), null, null, null, null);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findAll(any(Pageable.class));
    }

    @Test
    void listByStatus_basic() {
        when(transactionRepository.findByStatus(eq(TransactionStatus.AUTHORIZED), any(Pageable.class))).thenReturn(singlePage);
        Page<Transaction> page = controller.listByStatus(TransactionStatus.AUTHORIZED, 0, 20);
        assertEquals(1, page.getTotalElements());
        verify(transactionRepository).findByStatus(eq(TransactionStatus.AUTHORIZED), any(Pageable.class));
    }

    @Test
    void getStats_basic() {
        when(transactionRepository.count()).thenReturn(5L);
        when(transactionRepository.findByStatus(eq(TransactionStatus.AUTHORIZED), any(Pageable.class))).thenReturn(emptyPage);
        when(transactionRepository.findByStatus(eq(TransactionStatus.PAID), any(Pageable.class))).thenReturn(singlePage);
        when(transactionRepository.findByStatus(eq(TransactionStatus.REFUNDED), any(Pageable.class))).thenReturn(emptyPage);
        when(transactionRepository.findByStatus(eq(TransactionStatus.FAILED), any(Pageable.class))).thenReturn(emptyPage);
        TransactionsController.TransactionStats stats = controller.getStats();
        assertEquals(5, stats.total());
        assertEquals(0, stats.authorized());
        assertEquals(1, stats.paid());
        assertEquals(0, stats.refunded());
        assertEquals(0, stats.failed());
    }

    @Test
    void test_simpleString() {
        assertEquals("TransactionsController is working!", controller.test());
    }

    @Test
    void count_basic() {
        when(transactionRepository.count()).thenReturn(7L);
        assertEquals(7L, controller.count());
    }

    @Test
    void getFirst_withResult_and_without() {
        when(transactionRepository.findAll()).thenReturn(List.of(tx));
        TransactionDto dto = controller.getFirst();
        assertNotNull(dto);
        when(transactionRepository.findAll()).thenReturn(List.of());
        assertNull(controller.getFirst());
    }

    @Test
    void debug_withTx_noTx_and_exception() {
        when(transactionRepository.findAll()).thenReturn(List.of(tx));
        String ok = controller.debug();
        assertTrue(ok.contains("Transaction found"));
        when(transactionRepository.findAll()).thenReturn(List.of());
        assertEquals("No transactions found", controller.debug());
        when(transactionRepository.findAll()).thenThrow(new RuntimeException("db error"));
        String err = controller.debug();
        assertTrue(err.contains("Error: RuntimeException - db error"));
    }

    @Test
    void getRawStatus_success_and_exception() {
        when(transactionRepository.findRawStatusValues()).thenReturn(List.of("PAID", "FAILED"));
        List<String> list = controller.getRawStatus();
        assertEquals(2, list.size());
        when(transactionRepository.findRawStatusValues()).thenThrow(new RuntimeException("boom"));
        List<String> err = controller.getRawStatus();
        assertEquals(1, err.size());
        assertTrue(err.get(0).contains("Error: RuntimeException - boom"));
    }

    @Test
    void getSimpleTransaction_paths() {
        // empty
        when(transactionRepository.findSimpleTransactionData()).thenReturn(List.of());
        ResponseEntity<String> r1 = controller.getSimpleTransaction();
        assertEquals("No transactions found", r1.getBody());
        // one result
        when(transactionRepository.findSimpleTransactionData()).thenReturn(Collections.singletonList(new Object[]{UUID.randomUUID(), new BigDecimal("10.00"), "PAID"}));
        ResponseEntity<String> r2 = controller.getSimpleTransaction();
        assertEquals(200, r2.getStatusCode().value());
        assertTrue(r2.getBody().contains("ID:"));
        // exception
        when(transactionRepository.findSimpleTransactionData()).thenThrow(new RuntimeException("x"));
        ResponseEntity<String> r3 = controller.getSimpleTransaction();
        assertTrue(r3.getBody().contains("Error: RuntimeException - x"));
    }

    @Test
    void debugFilters_status_minAmount_none_exception() {
        when(transactionRepository.findByStatus(eq(TransactionStatus.PAID), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<String> r1 = controller.debugFilters("paid", null);
        assertTrue(r1.getBody().contains("Status filter works"));

        when(transactionRepository.countByAmountGreaterThanEqual(new BigDecimal("50"))).thenReturn(3L);
        ResponseEntity<String> r2 = controller.debugFilters(null, new BigDecimal("50"));
        assertTrue(r2.getBody().contains("Amount filter works"));

        ResponseEntity<String> r3 = controller.debugFilters(null, null);
        assertEquals("No filters provided", r3.getBody());

        when(transactionRepository.findByStatus(any(), any(Pageable.class))).thenThrow(new RuntimeException("oops"));
        ResponseEntity<String> r4 = controller.debugFilters("paid", null);
        assertTrue(r4.getBody().contains("Error: RuntimeException - oops"));
    }

    @Test
    void debugSimple_basic() {
        ResponseEntity<String> r = controller.debugSimple();
        assertEquals(200, r.getStatusCode().value());
        assertTrue(r.getBody().contains("Simple endpoint works"));
    }

    @Test
    void debugDateFilters_cases() {
        OffsetDateTime start = OffsetDateTime.now().minusDays(2);
        OffsetDateTime end = OffsetDateTime.now();
        when(transactionRepository.findByCreatedAtBetween(eq(start), eq(end), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<String> r1 = controller.debugDateFilters("s", "e", start, end);
        assertTrue(r1.getBody().contains("Query result: Found 1"));

        ResponseEntity<String> r2 = controller.debugDateFilters(null, null, null, null);
        assertTrue(r2.getBody().contains("Debug Date Filters"));

        when(transactionRepository.findByCreatedAtBetween(any(), any(), any(Pageable.class))).thenThrow(new RuntimeException("err"));
        ResponseEntity<String> r3 = controller.debugDateFilters("s", "e", start, end);
        assertTrue(r3.getBody().contains("Error: RuntimeException - err"));
    }

    @Test
    void debugAmountFilters_cases() {
        when(transactionRepository.findTransactionsByAmountRangeNative(any(), any())).thenReturn(Collections.singletonList(new Object[]{UUID.randomUUID(), new BigDecimal("10.00"), "PAID"}));
        ResponseEntity<String> r1 = controller.debugAmountFilters(new BigDecimal("1"), new BigDecimal("2"));
        assertTrue(r1.getBody().contains("Amount range filter works"));

        when(transactionRepository.findTransactionsByMinAmountNative(any())).thenReturn(List.of());
        ResponseEntity<String> r2 = controller.debugAmountFilters(new BigDecimal("5"), null);
        assertTrue(r2.getBody().contains("Min amount filter works"));

        when(transactionRepository.findTransactionsByMaxAmountNative(any())).thenReturn(List.of());
        ResponseEntity<String> r3 = controller.debugAmountFilters(null, new BigDecimal("9"));
        assertTrue(r3.getBody().contains("Max amount filter works"));

        ResponseEntity<String> r4 = controller.debugAmountFilters(null, null);
        assertEquals("No amount filters provided", r4.getBody());

        when(transactionRepository.findTransactionsByAmountRangeNative(any(), any())).thenThrow(new RuntimeException("boom"));
        ResponseEntity<String> r5 = controller.debugAmountFilters(new BigDecimal("1"), new BigDecimal("2"));
        assertTrue(r5.getBody().contains("Error: RuntimeException - boom"));
    }

    @Test
    void listNative_basicBranches_and_errors() {
        // search branch
        when(transactionRepository.findBySearchText(eq("q"), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<?> s1 = controller.listNative(0, 20, "createdAt", "desc", null, null, null, null, null, null, "q");
        assertEquals(200, ((ResponseEntity<?>) s1).getStatusCode().value());

        // no filters
        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<?> s2 = controller.listNative(0, 20, "createdAt", "desc", null, null, null, null, null, null, null);
        assertEquals(200, s2.getStatusCode().value());

        // status only valid
        when(transactionRepository.findByStatus(eq(TransactionStatus.PAID), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<?> s3 = controller.listNative(0, 20, "createdAt", "desc", "paid", null, null, null, null, null, null);
        assertEquals(200, s3.getStatusCode().value());

        // status invalid
        ResponseEntity<?> s4 = controller.listNative(0, 20, "createdAt", "desc", "invalid_status", null, null, null, null, null, null);
        assertEquals(400, s4.getStatusCode().value());

        // amount only
        when(transactionRepository.findByAmountBetween(any(), any(), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<?> s5 = controller.listNative(0, 20, "createdAt", "desc", null, null, new BigDecimal("1"), new BigDecimal("2"), null, null, null);
        assertEquals(200, s5.getStatusCode().value());

        // date only (both)
        OffsetDateTime start = OffsetDateTime.now().minusDays(2);
        OffsetDateTime end = OffsetDateTime.now();
        when(transactionRepository.findByCreatedAtBetween(eq(start), eq(end), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<?> s6 = controller.listNative(0, 20, "createdAt", "desc", null, null, null, null, start, end, null);
        assertEquals(200, s6.getStatusCode().value());

        // date only start
        when(transactionRepository.findByCreatedAtGreaterThanEqual(eq(start), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<?> s6a = controller.listNative(0, 20, "createdAt", "desc", null, null, null, null, start, null, null);
        assertEquals(200, s6a.getStatusCode().value());

        // date only end
        when(transactionRepository.findByCreatedAtLessThanEqual(eq(end), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<?> s6b = controller.listNative(0, 20, "createdAt", "desc", null, null, null, null, null, end, null);
        assertEquals(200, s6b.getStatusCode().value());

        // status + dates
        when(transactionRepository.findByStatusAndCreatedAtBetween(eq(TransactionStatus.PAID), eq(start), eq(end), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<?> s7 = controller.listNative(0, 20, "createdAt", "desc", "paid", null, null, null, start, end, null);
        assertEquals(200, s7.getStatusCode().value());

        // status + amounts
        when(transactionRepository.findByStatusAndAmountBetween(eq(TransactionStatus.PAID), any(), any(), any(Pageable.class))).thenReturn(singlePage);
        ResponseEntity<?> s8 = controller.listNative(0, 20, "createdAt", "desc", "paid", null, new BigDecimal("1"), new BigDecimal("2"), null, null, null);
        assertEquals(200, s8.getStatusCode().value());

        // exception
        when(transactionRepository.findAll(any(Pageable.class))).thenThrow(new RuntimeException("err"));
        ResponseEntity<?> s9 = controller.listNative(0, 20, "createdAt", "desc", null, null, null, null, null, null, null);
        assertEquals(500, s9.getStatusCode().value());
    }

    @Test
    void getTransactionDetail_statusVariants() {
        // AUTHORIZED => pending, no pdf
        Transaction tAuth = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("EXT-2")
                .amount(new BigDecimal("50"))
                .currency("ARS")
                .status(TransactionStatus.AUTHORIZED)
                .customerDoc("1")
                .capturedAt(OffsetDateTime.now())
                .build();
        when(transactionRepository.findById(any(UUID.class))).thenReturn(Optional.of(tAuth));
        ResponseEntity<?> r1 = controller.getTransactionDetail(UUID.randomUUID());
        assertEquals(200, r1.getStatusCode().value());
        TransactionsController.TransactionDetailDto d1 = (TransactionsController.TransactionDetailDto) r1.getBody();
        assertEquals("pending", d1.invoiceStatus());
        assertNull(d1.pdfUrl());

        // FAILED => error
        Transaction tFail = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("EXT-3")
                .amount(new BigDecimal("60"))
                .currency("ARS")
                .status(TransactionStatus.FAILED)
                .customerDoc("2")
                .capturedAt(OffsetDateTime.now())
                .build();
        when(transactionRepository.findById(any(UUID.class))).thenReturn(Optional.of(tFail));
        ResponseEntity<?> r2 = controller.getTransactionDetail(UUID.randomUUID());
        TransactionsController.TransactionDetailDto d2 = (TransactionsController.TransactionDetailDto) r2.getBody();
        assertEquals("error", d2.invoiceStatus());
        assertNull(d2.pdfUrl());
    }
}
