package com.gf.connector.service;

import com.gf.connector.domain.BillingSettings;
import com.gf.connector.dto.BillingSettingsDto;
import com.gf.connector.repo.BillingSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingSettingsServiceTest {

    @Mock private BillingSettingsRepository repo;
    @InjectMocks private BillingSettingsService service;

    private UUID tenant;

    @BeforeEach
    void setup() {
        tenant = UUID.randomUUID();
    }

    @Test
    void createSettings_setsDefaults_whenNulls() {
        BillingSettingsDto dto = new BillingSettingsDto();
        dto.setActivo(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingSettingsDto out = service.createSettings(dto, tenant);

        assertThat(out.getIvaPorDefecto()).isEqualByComparingTo(new BigDecimal("21.00"));
        assertThat(out.getTipoComprobante()).isEqualTo("FB");
        assertThat(out.getPuntoVenta()).isEqualTo("0001");
    }

    @Test
    void updateSettings_appliesFallbacks() {
        BillingSettings existing = BillingSettings.builder().id(UUID.randomUUID()).tenantId(tenant).build();
        when(repo.findById(any())).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingSettingsDto dto = new BillingSettingsDto();
        BillingSettingsDto out = service.updateSettings(existing.getId(), dto, tenant);

        assertThat(out.getIvaPorDefecto()).isEqualByComparingTo(new BigDecimal("21.00"));
        assertThat(out.getTipoComprobante()).isEqualTo("FB");
        assertThat(out.getPuntoVenta()).isEqualTo("0001");
    }
}

 

