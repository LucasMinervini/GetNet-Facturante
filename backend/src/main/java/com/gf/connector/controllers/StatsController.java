package com.gf.connector.controllers;

import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<?> getKpis(@RequestAttribute(name = "tenantId", required = false) java.util.UUID tenantId,
                                     @RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to) {
        if (tenantId == null) return ResponseEntity.status(401).build();
        OffsetDateTime start = from != null ? OffsetDateTime.parse(from) : OffsetDateTime.now().minusDays(7);
        OffsetDateTime end = to != null ? OffsetDateTime.parse(to) : OffsetDateTime.now();

        long total = transactionRepository.findByCreatedAtBetweenAndTenantId(start, end, tenantId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long paid = transactionRepository.findByStatusAndTenantId(TransactionStatus.PAID, tenantId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long refunded = transactionRepository.findByStatusAndTenantId(TransactionStatus.REFUNDED, tenantId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long authorized = transactionRepository.findByStatusAndTenantId(TransactionStatus.AUTHORIZED, tenantId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        long failed = transactionRepository.findByStatusAndTenantId(TransactionStatus.FAILED, tenantId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        // TODO: integrar métricas de webhooks y facturas desde repositorios correspondientes
        return ResponseEntity.ok(Map.of(
                "total", total,
                "authorized", authorized,
                "paid", paid,
                "refunded", refunded,
                "failed", failed
        ));
    }
}


