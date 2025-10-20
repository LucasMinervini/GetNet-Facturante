package com.gf.connector.integration;

import com.gf.connector.service.ReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ReconciliationServiceIT {

    @Autowired
    private ReconciliationService reconciliationService;

    @Test
    @DisplayName("Reconciliación ejecuta sin excepciones y retorna resultado válido")
    void performReconciliation_smoke() {
        UUID tenant = UUID.randomUUID();
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now();

        var result = reconciliationService.performReconciliation(tenant, start, end);

        assertThat(result).isNotNull();
        assertThat(result.getProcessedCount()).isGreaterThanOrEqualTo(0);
        assertThat(result.getErrorCount()).isGreaterThanOrEqualTo(0);
    }
}


