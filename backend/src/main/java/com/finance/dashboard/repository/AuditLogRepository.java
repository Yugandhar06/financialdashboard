package com.finance.dashboard.repository;

import com.finance.dashboard.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific entity (paginated)
     */
    Page<AuditLog> findByEntityIdAndEntityType(Long entityId, String entityType, Pageable pageable);

    /**
     * Find all audit logs for a specific entity, sorted by timestamp descending
     */
    List<AuditLog> findByEntityIdAndEntityTypeOrderByTimestampDesc(Long entityId, String entityType);

    /**
     * Find recent audit logs across all entities
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startTime AND :endTime ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentChanges(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Find all changes made by a specific user
     */
    Page<AuditLog> findByChangedByOrderByTimestampDesc(String changedBy, Pageable pageable);

    /**
     * Count changes to a specific entity
     */
    long countByEntityIdAndEntityType(Long entityId, String entityType);

    /**
     * Find audit logs by action type
     */
    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);
}
