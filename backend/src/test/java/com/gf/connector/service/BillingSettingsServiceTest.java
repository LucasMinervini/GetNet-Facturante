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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingSettingsServiceTest {

    @Mock private BillingSettingsRepository repo;
    @InjectMocks private BillingSettingsService service;

    private UUID tenant;
    private BillingSettingsDto validDto;
    private BillingSettings existingSettings;

    @BeforeEach
    void setup() {
        tenant = UUID.randomUUID();
        
        validDto = new BillingSettingsDto();
        validDto.setCuitEmpresa("20123456789");
        validDto.setRazonSocialEmpresa("Empresa Test");
        validDto.setIvaPorDefecto(new BigDecimal("21.00"));
        validDto.setTipoComprobante("FB");
        validDto.setPuntoVenta("0001");
        validDto.setActivo(true);
        validDto.setDescripcion("Configuración de prueba");
        
        existingSettings = BillingSettings.builder()
                .id(UUID.randomUUID())
                .tenantId(tenant)
                .cuitEmpresa("20123456789")
                .razonSocialEmpresa("Empresa Test")
                .build();
    }

    @Test
    void getActiveSettings_withExistingSettings_returnsSettings() {
        when(repo.findByActivoTrueAndTenantId(tenant)).thenReturn(Optional.of(existingSettings));

        Optional<BillingSettings> result = service.getActiveSettings(tenant);

        assertThat(result).isPresent();
        assertThat(result.get().getCuitEmpresa()).isEqualTo("20123456789");
        verify(repo).findByActivoTrueAndTenantId(tenant);
    }

    @Test
    void getActiveSettings_withNoSettings_returnsEmpty() {
        when(repo.findByActivoTrueAndTenantId(tenant)).thenReturn(Optional.empty());

        Optional<BillingSettings> result = service.getActiveSettings(tenant);

        assertThat(result).isEmpty();
    }

    @Test
    void getActiveSettingsDto_withExistingSettings_returnsDto() {
        when(repo.findByActivoTrueAndTenantId(tenant)).thenReturn(Optional.of(existingSettings));

        Optional<BillingSettingsDto> result = service.getActiveSettingsDto(tenant);

        assertThat(result).isPresent();
        assertThat(result.get().getCuitEmpresa()).isEqualTo("20123456789");
    }

    @Test
    void getAllSettings_returnsFilteredSettings() {
        BillingSettings settings1 = BillingSettings.builder().tenantId(tenant).build();
        BillingSettings settings2 = BillingSettings.builder().tenantId(UUID.randomUUID()).build();
        when(repo.findAll()).thenReturn(Arrays.asList(settings1, settings2));

        List<BillingSettingsDto> result = service.getAllSettings(tenant);

        assertThat(result).hasSize(1);
    }

    @Test
    void getSettingsById_withExistingSettings_returnsDto() {
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));

        Optional<BillingSettingsDto> result = service.getSettingsById(existingSettings.getId(), tenant);

        assertThat(result).isPresent();
        assertThat(result.get().getCuitEmpresa()).isEqualTo("20123456789");
    }

    @Test
    void getSettingsById_withDifferentTenant_returnsEmpty() {
        UUID differentTenant = UUID.randomUUID();
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));

        Optional<BillingSettingsDto> result = service.getSettingsById(existingSettings.getId(), differentTenant);

        assertThat(result).isEmpty();
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
    void createSettings_withActiveTrue_deactivatesOthers() {
        validDto.setActivo(true);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createSettings(validDto, tenant);

        verify(repo).deleteByActivoTrueAndTenantId(tenant);
        verify(repo).save(any(BillingSettings.class));
    }

    @Test
    void createSettings_withActiveFalse_doesNotDeactivateOthers() {
        validDto.setActivo(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createSettings(validDto, tenant);

        verify(repo, never()).deleteByActivoTrueAndTenantId(any());
        verify(repo).save(any(BillingSettings.class));
    }

    @Test
    void updateSettings_withExistingSettings_updatesSuccessfully() {
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingSettingsDto result = service.updateSettings(existingSettings.getId(), validDto, tenant);

        assertThat(result).isNotNull();
        verify(repo).save(existingSettings);
    }

    @Test
    void updateSettings_withNonExistentSettings_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(repo.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSettings(nonExistentId, validDto, tenant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Configuración no encontrada");
    }

    @Test
    void updateSettings_withDifferentTenant_throwsException() {
        UUID differentTenant = UUID.randomUUID();
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));

        assertThatThrownBy(() -> service.updateSettings(existingSettings.getId(), validDto, differentTenant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Configuración no pertenece al tenant");
    }

    @Test
    void updateSettings_withActiveTrue_deactivatesOthers() {
        validDto.setActivo(true);
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateSettings(existingSettings.getId(), validDto, tenant);

        verify(repo).deleteByActivoTrueAndIdNotAndTenantId(existingSettings.getId(), tenant);
    }

    @Test
    void updateSettings_appliesFallbacks() {
        BillingSettingsDto dto = new BillingSettingsDto();
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingSettingsDto out = service.updateSettings(existingSettings.getId(), dto, tenant);

        assertThat(out.getIvaPorDefecto()).isEqualByComparingTo(new BigDecimal("21.00"));
        assertThat(out.getTipoComprobante()).isEqualTo("FB");
        assertThat(out.getPuntoVenta()).isEqualTo("0001");
    }

    @Test
    void activateSettings_withExistingSettings_activatesSuccessfully() {
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingSettingsDto result = service.activateSettings(existingSettings.getId(), tenant);

        assertThat(result).isNotNull();
        verify(repo).deleteByActivoTrueAndIdNotAndTenantId(existingSettings.getId(), tenant);
        verify(repo).save(existingSettings);
        assertThat(existingSettings.getActivo()).isTrue();
    }

    @Test
    void activateSettings_withNonExistentSettings_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(repo.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateSettings(nonExistentId, tenant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Configuración no encontrada");
    }

    @Test
    void deleteSettings_withExistingSettings_deletesSuccessfully() {
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));

        service.deleteSettings(existingSettings.getId(), tenant);

        verify(repo).delete(existingSettings);
    }

    @Test
    void deleteSettings_withNonExistentSettings_throwsException() {
        UUID nonExistentId = UUID.randomUUID();
        when(repo.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSettings(nonExistentId, tenant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Configuración no encontrada");
    }

    @Test
    void deleteSettings_withDifferentTenant_throwsException() {
        UUID differentTenant = UUID.randomUUID();
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));

        assertThatThrownBy(() -> service.deleteSettings(existingSettings.getId(), differentTenant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Configuración no pertenece al tenant");
    }

    @Test
    void deleteSettings_withActiveSettingsAndOnlyOne_throwsException() {
        existingSettings.setActivo(true);
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));
        when(repo.existsByActivoTrueAndTenantId(tenant)).thenReturn(true);
        when(repo.count()).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteSettings(existingSettings.getId(), tenant))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede eliminar la única configuración activa");
    }

    @Test
    void createDefaultSettingsIfNotExists_whenNoActiveSettings_createsDefault() {
        when(repo.existsByActivoTrueAndTenantId(tenant)).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createDefaultSettingsIfNotExists(tenant);

        verify(repo).save(any(BillingSettings.class));
    }

    @Test
    void createDefaultSettingsIfNotExists_whenActiveSettingsExist_doesNotCreate() {
        when(repo.existsByActivoTrueAndTenantId(tenant)).thenReturn(true);

        service.createDefaultSettingsIfNotExists(tenant);

        verify(repo, never()).save(any(BillingSettings.class));
    }

    // Tests de compatibilidad (métodos sin tenantId)
    @Test
    void getActiveSettings_withoutTenant_usesDefaultTenant() {
        when(repo.findByActivoTrueAndTenantId(any())).thenReturn(Optional.of(existingSettings));

        Optional<BillingSettings> result = service.getActiveSettings();

        assertThat(result).isPresent();
        verify(repo).findByActivoTrueAndTenantId(any());
    }

    @Test
    void createSettings_withoutTenant_usesDefaultTenant() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingSettingsDto result = service.createSettings(validDto);

        assertThat(result).isNotNull();
        verify(repo).save(any(BillingSettings.class));
    }

    @Test
    void updateSettings_withoutTenant_usesDefaultTenant() {
        // Use the default tenant ID for the existing settings
        existingSettings.setTenantId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingSettingsDto result = service.updateSettings(existingSettings.getId(), validDto);

        assertThat(result).isNotNull();
        verify(repo).save(any(BillingSettings.class));
    }

    @Test
    void activateSettings_withoutTenant_usesDefaultTenant() {
        // Use the default tenant ID for the existing settings
        existingSettings.setTenantId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingSettingsDto result = service.activateSettings(existingSettings.getId());

        assertThat(result).isNotNull();
        verify(repo).save(any(BillingSettings.class));
    }

    @Test
    void deleteSettings_withoutTenant_usesDefaultTenant() {
        // Use the default tenant ID for the existing settings
        existingSettings.setTenantId(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
        when(repo.findById(existingSettings.getId())).thenReturn(Optional.of(existingSettings));

        service.deleteSettings(existingSettings.getId());

        verify(repo).delete(existingSettings);
    }

    @Test
    void createDefaultSettingsIfNotExists_withoutTenant_usesDefaultTenant() {
        when(repo.existsByActivoTrueAndTenantId(any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.createDefaultSettingsIfNotExists();

        verify(repo).save(any(BillingSettings.class));
    }
}

 

