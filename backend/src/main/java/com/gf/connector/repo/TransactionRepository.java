package com.gf.connector.repo;

import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByExternalId(String externalId);
    
    // Filtros básicos
    Page<Transaction> findByStatusAndTenantId(TransactionStatus status, java.util.UUID tenantId, Pageable pageable);
    Page<Transaction> findByBillingStatusAndTenantId(String billingStatus, java.util.UUID tenantId, Pageable pageable);
    Page<Transaction> findByAmountBetweenAndTenantId(BigDecimal minAmount, BigDecimal maxAmount, java.util.UUID tenantId, Pageable pageable);
    @Query(value = "SELECT COUNT(*) FROM transactions WHERE amount >= :minAmount", nativeQuery = true)
    Long countByAmountGreaterThanEqual(@Param("minAmount") BigDecimal minAmount);
    
    @Query(value = "SELECT * FROM transactions WHERE amount <= :maxAmount", nativeQuery = true)
    List<Transaction> findByAmountLessThanEqualNative(@Param("maxAmount") BigDecimal maxAmount);
    
    @Query(value = "SELECT id, amount, status FROM transactions LIMIT 5", nativeQuery = true)
    List<Object[]> findRawTransactionData();
    Page<Transaction> findByCreatedAtBetweenAndTenantId(OffsetDateTime startDate, OffsetDateTime endDate, java.util.UUID tenantId, Pageable pageable);
    Page<Transaction> findByCreatedAtGreaterThanEqualAndTenantId(OffsetDateTime startDate, java.util.UUID tenantId, Pageable pageable);
    Page<Transaction> findByCreatedAtLessThanEqualAndTenantId(OffsetDateTime endDate, java.util.UUID tenantId, Pageable pageable);
    
    // Filtros combinados
    Page<Transaction> findByStatusAndCreatedAtBetweenAndTenantId(TransactionStatus status, OffsetDateTime startDate, OffsetDateTime endDate, java.util.UUID tenantId, Pageable pageable);
    Page<Transaction> findByStatusAndAmountBetweenAndTenantId(TransactionStatus status, BigDecimal minAmount, BigDecimal maxAmount, java.util.UUID tenantId, Pageable pageable);
    
    // Query personalizada para filtros múltiples usando SQL nativo
    @Query(value = "SELECT * FROM transactions t WHERE " +
           "(:status IS NULL OR t.status = CAST(:status AS VARCHAR)) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
           "(:startDate IS NULL OR t.created_at >= :startDate) AND " +
           "(:endDate IS NULL OR t.created_at <= :endDate)",
           countQuery = "SELECT COUNT(*) FROM transactions t WHERE " +
           "(:status IS NULL OR t.status = CAST(:status AS VARCHAR)) AND " +
           "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
           "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
           "(:startDate IS NULL OR t.created_at >= :startDate) AND " +
           "(:endDate IS NULL OR t.created_at <= :endDate)",
           nativeQuery = true)
    Page<Transaction> findWithFilters(
            @Param("status") String status,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable
    );
    
    // Búsqueda por texto en externalId o customerDoc
    @Query("SELECT t FROM Transaction t WHERE t.tenantId = :tenantId AND (" +
           "LOWER(t.externalId) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "LOWER(t.customerDoc) LIKE LOWER(CONCAT('%', :searchText, '%')))")
    Page<Transaction> findBySearchText(@Param("searchText") String searchText, @Param("tenantId") java.util.UUID tenantId, Pageable pageable);

    // ===== Overloads sin tenant para compatibilidad de tests =====
    @Query("SELECT t FROM Transaction t WHERE " +
           "LOWER(t.externalId) LIKE LOWER(CONCAT('%', :searchText, '%')) OR " +
           "LOWER(t.customerDoc) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    Page<Transaction> findBySearchText(@Param("searchText") String searchText, Pageable pageable);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);

    Page<Transaction> findByCreatedAtBetween(OffsetDateTime startDate, OffsetDateTime endDate, Pageable pageable);

    Page<Transaction> findByCreatedAtGreaterThanEqual(OffsetDateTime startDate, Pageable pageable);

    Page<Transaction> findByCreatedAtLessThanEqual(OffsetDateTime endDate, Pageable pageable);

    Page<Transaction> findByStatusAndCreatedAtBetween(TransactionStatus status, OffsetDateTime startDate, OffsetDateTime endDate, Pageable pageable);

    Page<Transaction> findByStatusAndAmountBetween(TransactionStatus status, BigDecimal minAmount, BigDecimal maxAmount, Pageable pageable);
    
    // Query nativa para obtener valores de status directamente de la base de datos
    @Query(value = "SELECT DISTINCT status FROM transactions", nativeQuery = true)
    List<String> findRawStatusValues();
    
    // Query nativa para obtener datos simples sin el enum status
    @Query(value = "SELECT id, amount FROM transactions LIMIT 1", nativeQuery = true)
    List<Object[]> findSimpleTransactionData();
    
    // Query nativa para filtrar por amount sin mapear entidades
    @Query(value = "SELECT id, amount, status, external_id FROM transactions WHERE amount >= :minAmount ORDER BY created_at DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findTransactionsByMinAmountNative(@Param("minAmount") BigDecimal minAmount);
    
    @Query(value = "SELECT id, amount, status, external_id FROM transactions WHERE amount <= :maxAmount ORDER BY created_at DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findTransactionsByMaxAmountNative(@Param("maxAmount") BigDecimal maxAmount);
    
    @Query(value = "SELECT id, amount, status, external_id FROM transactions WHERE amount BETWEEN :minAmount AND :maxAmount ORDER BY created_at DESC LIMIT 10", nativeQuery = true)
    List<Object[]> findTransactionsByAmountRangeNative(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);
    
    // Método para eliminar transacciones de prueba
    void deleteByExternalIdStartingWith(String prefix);
}
