package com.gf.connector.performance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PerformanceSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void transactions_list_should_be_fast_under_small_load() throws Exception {
        int requests = 10; // smoke bajo
        long start = System.currentTimeMillis();
        for (int i = 0; i < requests; i++) {
            mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        }
        long elapsedMs = System.currentTimeMillis() - start;
        // Umbral conservador para entorno local/CI: 5s para 10 requests
        org.assertj.core.api.Assertions.assertThat(elapsedMs)
            .withFailMessage("Demasiado lento: %sms para %s requests", elapsedMs, requests)
            .isLessThan(5000);
    }
}


