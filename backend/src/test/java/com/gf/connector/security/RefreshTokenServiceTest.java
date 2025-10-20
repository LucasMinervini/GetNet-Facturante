package com.gf.connector.security;

import com.gf.connector.domain.RefreshToken;
import com.gf.connector.domain.User;
import com.gf.connector.repo.RefreshTokenRepository;
import com.gf.connector.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repo;
    @InjectMocks private RefreshTokenService service;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder().username("u").build();
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createRefreshToken_revokesFamily_thenSaves() {
        RefreshToken token = service.createRefreshToken(user, "fam");
        assertThat(token.getUser()).isEqualTo(user);
        verify(repo).revokeFamilyTokens(eq("fam"), any(OffsetDateTime.class));
        verify(repo).save(any());
    }

    @Test
    void validateAndRotateToken_validToken_rotates() {
        RefreshToken existing = RefreshToken.builder().user(user).familyId("fam").expiresAt(OffsetDateTime.now().plusDays(1)).build();
        when(repo.findByTokenHashAndIsRevokedFalse(anyString())).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Optional<RefreshToken> rotated = service.validateAndRotateToken("raw");
        assertThat(rotated).isPresent();
        verify(repo, atLeastOnce()).save(any());
    }
}


