package com.resumade.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.resumade.auth.dto.NotificationEvent;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private NotificationProducer notificationProducer;

    @Test
    void sendNotification_shouldSendToExchange() {
        ReflectionTestUtils.setField(notificationProducer, "exchange", "notification-exchange");
        ReflectionTestUtils.setField(notificationProducer, "routingKey", "notification-routing-key");

        NotificationEvent event = new NotificationEvent(1, "user@example.com", "SYSTEM",
                "Welcome", "Hello", "EMAIL");

        notificationProducer.sendNotification(event);

        verify(rabbitTemplate).convertAndSend("notification-exchange", "notification-routing-key", event);
    }
}
