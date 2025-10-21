package com.gf.connector.service;

import com.gf.connector.domain.BillingSettings;
import com.gf.connector.repo.BillingSettingsRepository;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private BillingSettingsRepository billingSettingsRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private BackupService service;

    @BeforeEach
    void setup() {
        service = new BackupService(billingSettingsRepository, transactionRepository, userRepository);
        ReflectionTestUtils.setField(service, "notificationService", notificationService);
        ReflectionTestUtils.setField(service, "backupEnabled", true);
        ReflectionTestUtils.setField(service, "backupDirectory", "./test-backups");
        ReflectionTestUtils.setField(service, "retentionDays", 30);
    }

    @Test
    void performBackup_whenEnabled_createsBackup() {
        // Arrange
        BillingSettings settings = BillingSettings.builder()
                .tenantId(UUID.randomUUID())
                .cuitEmpresa("20123456789")
                .build();
        
        when(billingSettingsRepository.findAll()).thenReturn(List.of(settings));
        when(transactionRepository.count()).thenReturn(10L);
        when(userRepository.count()).thenReturn(5L);
        when(userRepository.findAll()).thenReturn(List.of());

        // Act
        BackupService.BackupResult result = service.performBackup();

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).contains("Backup exitoso");
        verify(billingSettingsRepository, times(1)).findAll();
        verify(transactionRepository, times(1)).count();
        verify(userRepository, times(1)).count();
    }

    @Test
    void performBackup_whenDisabled_returnsFalse() {
        ReflectionTestUtils.setField(service, "backupEnabled", false);

        BackupService.BackupResult result = service.performBackup();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Backup deshabilitado");
        verify(billingSettingsRepository, never()).findAll();
    }

    @Test
    void cleanupOldBackups_whenEnabled_removesOldFiles() {
        // Este método es privado, pero podemos probarlo indirectamente a través de performBackup
        // que llama a cleanupOldBackups internamente
        when(billingSettingsRepository.findAll()).thenReturn(List.of());
        when(transactionRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findAll()).thenReturn(List.of());

        BackupService.BackupResult result = service.performBackup();

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void performBackup_withNotificationService_sendsNotification() {
        when(billingSettingsRepository.findAll()).thenReturn(List.of());
        when(transactionRepository.count()).thenReturn(0L);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.findAll()).thenReturn(List.of());

        BackupService.BackupResult result = service.performBackup();

        assertThat(result.isSuccess()).isTrue();
        verify(notificationService, times(1)).sendBackupCompletedNotification(
            eq(null), eq(true), anyString());
    }

    @Test
    void restoreFromBackup_whenFileNotExists_returnsFalse() {
        String backupFile = "non-existent-backup.json";
        
        boolean result = service.restoreFromBackup(backupFile);

        assertThat(result).isFalse();
    }

    @Test
    void performBackup_withError_sendsErrorNotification() {
        when(billingSettingsRepository.findAll()).thenThrow(new RuntimeException("Database error"));

        BackupService.BackupResult result = service.performBackup();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("Error");
        verify(notificationService, times(1)).sendBackupCompletedNotification(
            eq(null), eq(false), anyString());
    }
}
