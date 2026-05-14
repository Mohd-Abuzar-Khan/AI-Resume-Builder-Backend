package com.resumade.ai.repository;

import com.resumade.ai.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AiRequestRepository extends JpaRepository<AiRequest, UUID> {

    List<AiRequest> findByUserId(Integer userId);

    @Query("SELECT COUNT(a) FROM AiRequest a WHERE a.userId = :userId AND a.createdAt >= :startDate")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Integer userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(a.tokensUsed) FROM AiRequest a WHERE a.userId = :userId")
    Long sumTokensByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT COUNT(a) FROM AiRequest a WHERE a.userId = :userId AND a.requestType = :type AND a.createdAt >= :startDate")
    long countByUserIdAndRequestTypeAndCreatedAtAfter(
            @Param("userId") Integer userId, 
            @Param("type") AiRequest.RequestType type, 
            @Param("startDate") LocalDateTime startDate);
}
