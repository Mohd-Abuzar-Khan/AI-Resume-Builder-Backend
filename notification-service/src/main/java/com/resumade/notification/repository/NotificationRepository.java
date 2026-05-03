package com.resumade.notification.repository;

import com.resumade.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByRecipientIdOrderBySentAtDesc(Integer recipientId);
    
    List<Notification> findByRecipientIdAndIsReadOrderBySentAtDesc(Integer recipientId, boolean isRead);
    
    long countByRecipientIdAndIsRead(Integer recipientId, boolean isRead);
    
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :userId")
    void markAllReadForUser(@Param("userId") Integer userId);
}
