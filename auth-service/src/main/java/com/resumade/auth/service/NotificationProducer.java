package com.resumade.auth.service;

import com.resumade.auth.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationProducer.class);

    @Value("${notification.exchange:notification-exchange}")
    private String exchange;

    @Value("${notification.routing-key:notification-routing-key}")
    private String routingKey;

    private final RabbitTemplate rabbitTemplate;

    public NotificationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendNotification(NotificationEvent event) {
        log.info("Sending notification event to RabbitMQ: {}", event.getTitle());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
