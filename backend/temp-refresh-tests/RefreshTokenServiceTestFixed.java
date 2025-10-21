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
    void revokeToken_foundToken_revokesIt() {
        // Usar anyString() para el hash ya que el método hashToken es privado
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(anyString())).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> {
            RefreshToken token = i.getArgument(0);
            // Simular que el token se marca como revocado
            token.setIsRevoked(true);
            token.setRevokedAt(OffsetDateTime.now());
            return token;
        });

        service.revokeToken(testToken);

        verify(refreshTokenRepository).findByTokenHashAndIsRevokedFalse(anyString());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void revokeToken_tokenNotFound_doesNothing() {
        // Usar anyString() para el hash ya que el método hashToken es privado
        when(refreshTokenRepository.findByTokenHashAndIsRevokedFalse(anyString())).thenReturn(Optional.empty());

        service.revokeToken(testToken);

        verify(refreshTokenRepository).findByTokenHashAndIsRevokedFalse(anyString());
        verify(refreshTokenRepository, never()).save(any());
    }
}
