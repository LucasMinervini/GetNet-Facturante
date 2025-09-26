package com.gf.connector.repo;

import com.gf.connector.domain.CreditNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, UUID> {
    
    /**
     * Buscar nota de crédito por transacción
     */
    Optional<CreditNote> findByTransactionId(UUID transactionId);
    
    /**
     * Buscar notas de crédito por estado
     */
    List<CreditNote> findByStatus(String status);
    
    /**
     * Buscar notas de crédito por estrategia
     */
    List<CreditNote> findByStrategy(String strategy);
    
    /**
     * Buscar notas de crédito por número
     */
    Optional<CreditNote> findByCreditNoteNumber(String creditNoteNumber);
    
    /**
     * Contar notas de crédito por estado
     */
    @Query("SELECT COUNT(cn) FROM CreditNote cn WHERE cn.status = :status")
    long countByStatus(@Param("status") String status);
    
    /**
     * Contar notas de crédito por estrategia
     */
    @Query("SELECT COUNT(cn) FROM CreditNote cn WHERE cn.strategy = :strategy")
    long countByStrategy(@Param("strategy") String strategy);
}
