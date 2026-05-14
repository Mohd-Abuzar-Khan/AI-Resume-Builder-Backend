package com.resumade.notification.service;

import com.resumade.notification.dto.NotificationEvent;
import com.resumade.notification.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

        @Mock
        private NotificationService notificationService;

        @InjectMocks
        private NotificationConsumer notificationConsumer;

        private NotificationEvent event;

        @BeforeEach
        void setUp() {
                event = new NotificationEvent();
                event.setUserId(1);
                event.setRecipientEmail("test@test.com");
                event.setTitle("Title");
                event.setMessage("Message");
        }

        @Test
        void consumeMessage_success() {
                event.setType("SYSTEM");
                event.setChannel("IN_APP");

                notificationConsumer.consumeMessage(event);

                verify(notificationService).createNotification(
                                1, "test@test.com", Notification.NotificationType.SYSTEM,
                                "Title", "Message", Notification.NotificationChannel.IN_APP);
        }

        @ParameterizedTest
        @CsvSource({
                        "INVALID_TYPE, INVALID_CHANNEL",
                        ", IN_APP",
                        "SYSTEM, "
        })
        void consumeMessage_fallbackOnInvalidOrNullEnum(String type, String channel) {
                event.setType(type);
                event.setChannel(channel);

                notificationConsumer.consumeMessage(event);

                verify(notificationService).createNotification(
                                1, null, Notification.NotificationType.SYSTEM,
                                "Title", "Message", Notification.NotificationChannel.IN_APP);
        }

        @Test
        void consumeMessage_successWithLowercase() {
                event.setType("system");
                event.setChannel("in_app");

                notificationConsumer.consumeMessage(event);

                verify(notificationService).createNotification(
                                1, "test@test.com", Notification.NotificationType.SYSTEM,
                                "Title", "Message", Notification.NotificationChannel.IN_APP);
        }

        @Test
        void consumeMessage_fallbackOnServiceException() {
                event.setType("SYSTEM");
                event.setChannel("IN_APP");

                // First call throws exception
                org.mockito.Mockito.doThrow(new RuntimeException("DB Error"))
                                .when(notificationService).createNotification(
                                                1, "test@test.com", Notification.NotificationType.SYSTEM,
                                                "Title", "Message", Notification.NotificationChannel.IN_APP);

                notificationConsumer.consumeMessage(event);

                // Fallback should be called with null recipientEmail
                verify(notificationService).createNotification(
                                1, null, Notification.NotificationType.SYSTEM,
                                "Title", "Message", Notification.NotificationChannel.IN_APP);
        }
}
