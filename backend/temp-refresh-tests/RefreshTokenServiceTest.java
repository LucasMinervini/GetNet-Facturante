package com.gf.connector.service;

import com.gf.connector.domain.RefreshToken;
import com.gf.connector.domain.User;
import com.gf.connector.repo.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks private RefreshTokenService service;

    private User user;
    private RefreshToken existingToken;
    private String testToken;
    private String testTokenHash;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();

        existingToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("hashed-token")
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .familyId("family-123")
                .isRevoked(false)
                .build();

        testToken = "test-token-123";
        testTokenHash = "hashed-test-token";
    }

    @Test
    void createRefreshToken_withoutFamilyId_createsNewToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken result = service.createRefreshToken(user, null);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getFamilyId()).isNotNull();
        assertThat(result.getExpiresAt()).isAfter(OffsetDateTime.now());
        assertThat(result.getIsRevoked()).isFalse();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_withFamilyId_revokesExistingTokens() {
        String familyId = "existing-family";
        when(refreshTokenRepository.revokeFamilyTokens(eq(familyId), any(OffsetDateTime.class))).thenReturn(1);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken result = service.createRefreshToken(user, familyId);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getFamilyId()).isEqualTo(familyId);
        verify(refreshTokenRepository).revokeFamilyTokens(eq(familyId), any(OffsetDateTime.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void validateAndRotateToken_validToken_returnsNewToken() {
        RefreshToken newToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("new-hash")
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .familyId("family-123")
                .isRevoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(eq(testTokenHash))).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        // Mock the createRefreshToken method call
        RefreshTokenService spyService = spy(service);
        doReturn(newToken).when(spyService).createRefreshToken(any(User.class), anyString());

        Optional<RefreshToken> result = spyService.validateAndRotateToken(testToken);

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(newToken);
        verify(refreshTokenRepository).findByTokenHashAndIsRevokedFalse(eq(testTokenHash));
        verify(refreshTokenRepository).save(existingToken);
    }

    @Test
    void validateAndRotateToken_invalidToken_returnsEmpty() {
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(eq(testTokenHash))).thenReturn(Optional.empty());

        Optional<RefreshToken> result = service.validateAndRotateToken(testToken);

        assertThat(result).isEmpty();
        verify(refreshTokenRepository).findByTokenHashAndIsRevokedFalse(eq(testTokenHash));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void validateAndRotateToken_expiredToken_returnsEmpty() {
        existingToken.setExpiresAt(OffsetDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(eq(testTokenHash))).thenReturn(Optional.of(existingToken));

        Optional<RefreshToken> result = service.validateAndRotateToken(testToken);

        assertThat(result).isEmpty();
        verify(refreshTokenRepository).findByTokenHashAndIsRevokedFalse(eq(testTokenHash));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeUserTokens_callsRepository() {
        service.revokeUserTokens(user);

        verify(refreshTokenRepository).revokeAllUserTokens(eq(user), any(OffsetDateTime.class));
    }

    @Test
    void revokeToken_foundToken_revokesIt() {
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(eq(testTokenHash))).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> {
            RefreshToken token = i.getArgument(0);
            // Simular que el token se marca como revocado
            token.setIsRevoked(true);
            token.setRevokedAt(OffsetDateTime.now());
            return token;
        });

        service.revokeToken(testToken);

        verify(refreshTokenRepository).findByTokenHashAndIsRevokedFalse(eq(testTokenHash));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void revokeToken_tokenNotFound_doesNothing() {
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(eq(testTokenHash))).thenReturn(Optional.empty());

        service.revokeToken(testToken);

        verify(refreshTokenRepository).findByTokenHashAndIsRevokedFalse(eq(testTokenHash));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void cleanupExpiredTokens_deletesExpiredTokens() {
        when(refreshTokenRepository.deleteExpiredTokens(any(OffsetDateTime.class))).thenReturn(5);

        service.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredTokens(any(OffsetDateTime.class));
    }

    @Test
    void cleanupExpiredTokens_noExpiredTokens_logsNothing() {
        when(refreshTokenRepository.deleteExpiredTokens(any(OffsetDateTime.class))).thenReturn(0);

        service.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredTokens(any(OffsetDateTime.class));
    }

    @Test
    void createRefreshToken_generatesUniqueTokens() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken token1 = service.createRefreshToken(user, null);
        RefreshToken token2 = service.createRefreshToken(user, null);

        // Los tokens deben ser diferentes (diferentes hashes)
        assertThat(token1.getTokenHash()).isNotEqualTo(token2.getTokenHash());
        assertThat(token1.getTokenHash()).isNotNull();
        assertThat(token2.getTokenHash()).isNotNull();
    }

    @Test
    void createRefreshToken_sameFamilyId_createsDifferentTokens() {
        String familyId = "test-family";
        when(refreshTokenRepository.revokeFamilyTokens(eq(familyId), any(OffsetDateTime.class))).thenReturn(0);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken token1 = service.createRefreshToken(user, familyId);
        RefreshToken token2 = service.createRefreshToken(user, familyId);

        // Los tokens deben ser diferentes pero con la misma familia
        assertThat(token1.getTokenHash()).isNotEqualTo(token2.getTokenHash());
        assertThat(token1.getFamilyId()).isEqualTo(familyId);
        assertThat(token2.getFamilyId()).isEqualTo(familyId);
    }
}
