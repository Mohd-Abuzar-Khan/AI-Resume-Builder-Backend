package com.resumade.notification.controller;

import com.resumade.notification.entity.Notification;
import com.resumade.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        mockNotification = new Notification(1, Notification.NotificationType.SYSTEM, "Test", "Msg", Notification.NotificationChannel.IN_APP);
    }

    @Test
    void getNotifications_shouldReturnList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(notificationService.getUserNotifications(1)).thenReturn(Arrays.asList(mockNotification));

        ResponseEntity<List<Notification>> response = notificationController.getNotifications(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(notificationService.getUnreadCount(1)).thenReturn(5L);

        ResponseEntity<Long> response = notificationController.getUnreadCount(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody());
    }

    @Test
    void markRead_shouldReturnOk() {
        ResponseEntity<Void> response = notificationController.markRead(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).markAsRead(1L);
    }

    @Test
    void markAllRead_shouldReturnOk() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);

        ResponseEntity<Void> response = notificationController.markAllRead(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).markAllRead(1);
    }

    @Test
    void broadcast_shouldReturnOk() {
        ResponseEntity<Void> response = notificationController.broadcast("Title", "Msg", "ALL");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).broadcastNotification("Title", "Msg", "ALL");
    }
}
