package com.resumade.export.repository;

import com.resumade.export.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ExportRepository extends JpaRepository<ExportJob, UUID> {
    
    List<ExportJob> findByUserId(Integer userId);
    
    List<ExportJob> findByStatus(ExportJob.ExportStatus status);
    
    @Query("SELECT e FROM ExportJob e WHERE e.expiresAt < :now")
    List<ExportJob> findExpiredJobs(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(e) FROM ExportJob e WHERE e.userId = :userId AND e.requestedAt >= :since")
    long countByUserIdToday(@Param("userId") Integer userId, @Param("since") LocalDateTime since);
}
