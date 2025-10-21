package com.gf.connector.service;

import com.gf.connector.domain.RefreshToken;
import com.gf.connector.domain.User;
import com.gf.connector.repo.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTestFailing {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService service;
    private User user;
    private RefreshToken existingToken;
    private String testToken;
    private String testTokenHash;

    @BeforeEach
    void setup() {
        service = new RefreshTokenService(refreshTokenRepository);
        
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .build();
        
        testToken = "test-refresh-token";
        testTokenHash = "hashed-token";
        
        existingToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(testTokenHash)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .familyId("family-123")
                .isRevoked(false)
                .build();
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
    void validateAndRotateToken_expiredToken_returnsEmpty() {
        existingToken.setExpiresAt(OffsetDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(eq(testTokenHash))).thenReturn(Optional.of(existingToken));

        Optional<RefreshToken> result = service.validateAndRotateToken(testToken);

        assertThat(result).isEmpty();
        verify(refreshTokenRepository).findByTokenHashAndIsRevokedFalse(eq(testTokenHash));
        verify(refreshTokenRepository, never()).save(any());
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
}
