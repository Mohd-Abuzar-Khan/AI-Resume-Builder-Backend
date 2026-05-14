package com.resumade.notification.service;

import com.resumade.notification.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
// RabbitMQ consumer — listens for notification events published by auth-service
// and other producers
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);
    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${notification.queue}")
    public void consumeMessage(NotificationEvent event) {
        log.info("Received notification event: {}", event.getTitle());
        
        try {
            com.resumade.notification.entity.Notification.NotificationType type = 
                com.resumade.notification.entity.Notification.NotificationType.valueOf(event.getType().toUpperCase());
            
            com.resumade.notification.entity.Notification.NotificationChannel channel = 
                com.resumade.notification.entity.Notification.NotificationChannel.valueOf(event.getChannel().toUpperCase());

            notificationService.createNotification(
                    event.getUserId(),
                    event.getRecipientEmail(),
                    type,
                    event.getTitle(),
                    event.getMessage(),
                    channel
            );
        } catch (Exception e) {
            // Graceful degradation: invalid type/channel enum falls back to SYSTEM/IN_APP
            log.error("Error processing notification event: {}. Using default values.", e.getMessage());
            notificationService.createNotification(
                    event.getUserId(),
                    null,
                    com.resumade.notification.entity.Notification.NotificationType.SYSTEM,
                    event.getTitle(),
                    event.getMessage(),
                    com.resumade.notification.entity.Notification.NotificationChannel.IN_APP
            );
        }
    }
}
