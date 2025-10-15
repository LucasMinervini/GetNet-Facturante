package com.gf.connector.repo;

import com.gf.connector.domain.RefreshToken;
import com.gf.connector.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    Optional<RefreshToken> findByTokenHashAndIsRevokedFalse(String tokenHash);
    
    List<RefreshToken> findByUserAndIsRevokedFalse(User user);
    
    List<RefreshToken> findByFamilyIdAndIsRevokedFalse(String familyId);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.revokedAt = :revokedAt WHERE rt.user = :user")
    int revokeAllUserTokens(@Param("user") User user, @Param("revokedAt") OffsetDateTime revokedAt);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true, rt.revokedAt = :revokedAt WHERE rt.familyId = :familyId")
    int revokeFamilyTokens(@Param("familyId") String familyId, @Param("revokedAt") OffsetDateTime revokedAt);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") OffsetDateTime cutoff);
}
