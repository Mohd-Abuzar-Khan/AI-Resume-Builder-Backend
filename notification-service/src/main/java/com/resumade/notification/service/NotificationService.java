package com.resumade.notification.service;

import com.resumade.notification.entity.Notification;
import java.util.List;

public interface NotificationService {
    Notification createNotification(Integer userId, String recipientEmail, Notification.NotificationType type, String title, String message, Notification.NotificationChannel channel);
    List<Notification> getUserNotifications(Integer userId);
    long getUnreadCount(Integer userId);
    void markAsRead(Long id);
    void markAllRead(Integer userId);
    void broadcastNotification(String title, String message, String recipientType);
}
