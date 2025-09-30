package com.gf.connector.service;

import com.gf.connector.domain.RefreshToken;
import com.gf.connector.domain.User;
import com.gf.connector.repo.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Transactional
    public RefreshToken createRefreshToken(User user, String familyId) {
        // Revocar tokens existentes de la misma familia
        if (familyId != null) {
            refreshTokenRepository.revokeFamilyTokens(familyId, OffsetDateTime.now());
        }
        
        String token = generateSecureToken();
        String tokenHash = hashToken(token);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusDays(7)) // 7 días
                .familyId(familyId != null ? familyId : UUID.randomUUID().toString())
                .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    @Transactional
    public Optional<RefreshToken> validateAndRotateToken(String token) {
        String tokenHash = hashToken(token);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash);
        
        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            return Optional.empty();
        }
        
        RefreshToken refreshToken = tokenOpt.get();
        
        // Rotación: revocar el token actual y crear uno nuevo
        refreshToken.setIsRevoked(true);
        refreshToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(refreshToken);
        
        // Crear nuevo token con la misma familia
        RefreshToken newToken = createRefreshToken(refreshToken.getUser(), refreshToken.getFamilyId());
        return Optional.of(newToken);
    }
    
    @Transactional
    public void revokeUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user, OffsetDateTime.now());
        log.info("Todos los tokens del usuario {} han sido revocados", user.getUsername());
    }
    
    @Transactional
    public void revokeToken(String token) {
        String tokenHash = hashToken(token);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash);
        if (tokenOpt.isPresent()) {
            RefreshToken refreshToken = tokenOpt.get();
            refreshToken.setIsRevoked(true);
            refreshToken.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(refreshToken);
        }
    }
    
    @Scheduled(fixedRate = 3600000) // Cada hora
    @Transactional
    public void cleanupExpiredTokens() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(1);
        int deleted = refreshTokenRepository.deleteExpiredTokens(cutoff);
        if (deleted > 0) {
            log.info("Limpiados {} tokens expirados", deleted);
        }
    }
    
    private String generateSecureToken() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }
    
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al hashear token", e);
        }
    }
}
