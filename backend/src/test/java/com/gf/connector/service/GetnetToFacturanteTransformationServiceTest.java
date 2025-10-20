package com.gf.connector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.domain.Transaction;
import com.gf.connector.facturante.config.FacturanteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GetnetToFacturanteTransformationServiceTest {

    @Mock private FacturanteConfig facturanteConfig;
    @Mock private ObjectMapper objectMapper;
    @Mock private BillingSettingsService billingSettingsService;

    @InjectMocks private GetnetToFacturanteTransformationService service;

    private Map<String, Object> simplePayload;

    @BeforeEach
    void setup() {
        simplePayload = Map.of("id", "T-1", "status", "PAID", "amount", 100, "currency", "ARS");
    }

    @Test
    void transformWebhookToTransaction_simpleFormat_mapsFields() {
        Transaction tx = service.transformWebhookToTransaction("{}", simplePayload);
        assertThat(tx.getExternalId()).isEqualTo("T-1");
        assertThat(tx.getAmount()).isNotNull();
        assertThat(tx.getCurrency()).isEqualTo("ARS");
    }

    @Test
    void transformWebhookToTransaction_genericFormat_usesFallbacks() {
        java.util.Map<String, Object> generic = new java.util.HashMap<>();
        generic.put("transaction_id", "G1");
        generic.put("status", "APPROVED");
        generic.put("amount", "10");
        Transaction tx = service.transformWebhookToTransaction("{}", generic);
        assertThat(tx.getExternalId()).isEqualTo("G1");
        assertThat(tx.getCurrency()).isEqualTo("ARS");
    }
}


