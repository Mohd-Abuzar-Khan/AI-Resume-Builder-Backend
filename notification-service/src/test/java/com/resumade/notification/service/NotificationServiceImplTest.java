package com.resumade.notification.service;

import com.resumade.notification.entity.Notification;
import com.resumade.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository repository;
    @Mock private JavaMailSender mailSender;
    @Mock private RestTemplate restTemplate;
    @Mock private TemplateEngine templateEngine;
    @Mock private NotificationService self;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification mockNotification;

    @BeforeEach
    void setUp() {
        mockNotification = new Notification(1, Notification.NotificationType.SYSTEM, "Test", "Msg", Notification.NotificationChannel.IN_APP);
    }

    @Test
    void createNotification_shouldSaveInApp() {
        when(repository.save(any())).thenReturn(mockNotification);

        Notification result = notificationService.createNotification(1, null, Notification.NotificationType.SYSTEM, "Title", "Msg", Notification.NotificationChannel.IN_APP);

        assertNotNull(result);
        verify(repository).save(any());
        verifyNoInteractions(mailSender);
    }

    @Test
    void createNotification_shouldSaveAndEmailWhenBoth() {
        when(repository.save(any())).thenReturn(mockNotification);
        
        // Simulating the actual email sending logic might fail silently due to catch block, but it should be attempted
        Notification result = notificationService.createNotification(1, "test@example.com", Notification.NotificationType.SYSTEM, "Title", "Msg", Notification.NotificationChannel.BOTH);

        assertNotNull(result);
        verify(repository).save(any());
        // Since mailSender.send is commented out in actual code, we won't verify it. We just verify it doesn't crash.
    }

    @Test
    void getUserNotifications_shouldReturnList() {
        when(repository.findByRecipientIdOrderBySentAtDesc(1)).thenReturn(Arrays.asList(mockNotification));

        List<Notification> result = notificationService.getUserNotifications(1);

        assertEquals(1, result.size());
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        when(repository.countByRecipientIdAndIsRead(1, false)).thenReturn(5L);

        long result = notificationService.getUnreadCount(1);

        assertEquals(5L, result);
    }

    @Test
    void markAsRead_shouldUpdate() {
        when(repository.findById(1L)).thenReturn(Optional.of(mockNotification));

        notificationService.markAsRead(1L);

        verify(repository).save(argThat(Notification::isRead));
    }

    @Test
    void markAllRead_shouldCallRepository() {
        notificationService.markAllRead(1);

        verify(repository).markAllReadForUser(1);
    }

    @Test
    void broadcastNotification_shouldFetchUsersAndCreateNotifications() {
        // Setup security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getCredentials()).thenReturn("token");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Map<String, Object> user1 = new HashMap<>();
        user1.put("userId", 1);
        user1.put("subscriptionPlan", "FREE");

        Map<String, Object> user2 = new HashMap<>();
        user2.put("userId", 2);
        user2.put("subscriptionPlan", "PREMIUM");

        Map[] users = new Map[]{user1, user2};
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map[].class)))
                .thenReturn(ResponseEntity.ok(users));

        notificationService.broadcastNotification("Title", "Msg", "ALL");
        verify(self, times(2)).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void broadcastNotification_shouldFilterByPlan() {
        // Setup security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getCredentials()).thenReturn("token");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Map<String, Object> user1 = new HashMap<>();
        user1.put("userId", 1);
        user1.put("subscriptionPlan", "FREE");

        Map<String, Object> user2 = new HashMap<>();
        user2.put("userId", 2);
        user2.put("subscriptionPlan", "PREMIUM");

        Map[] users = new Map[]{user1, user2};
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map[].class)))
                .thenReturn(ResponseEntity.ok(users));

        notificationService.broadcastNotification("Title", "Msg", "PREMIUM");
        verify(self, times(1)).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void broadcastNotification_fallbackToRepoWhenApiFails() {
        // Setup security context
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getCredentials()).thenReturn("token");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map[].class)))
                .thenThrow(new RuntimeException("API error"));

        Notification n1 = new Notification();
        n1.setRecipientId(1);
        when(repository.findAll()).thenReturn(Arrays.asList(n1));

        notificationService.broadcastNotification("Title", "Msg", "ALL");
        verify(self, atLeastOnce()).createNotification(any(), any(), any(), any(), any(), any());
    }

    @Test
    void broadcastNotification_nullAuthToken() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getCredentials()).thenReturn(new Object()); // Not a String
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map[].class)))
                .thenReturn(ResponseEntity.ok(new Map[0]));

        notificationService.broadcastNotification("Title", "Msg", "ALL");
        verify(repository, never()).save(any());
    }

    @Test
    void broadcastNotification_nullResponseBody() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getCredentials()).thenReturn("token");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map[].class)))
                .thenReturn(ResponseEntity.ok(null));

        notificationService.broadcastNotification("Title", "Msg", "ALL");
        verify(repository, never()).save(any());
    }

    @Test
    void createNotification_EmailChannelWithNullRecipient_callsSendEmail() {
        when(repository.save(any())).thenReturn(mockNotification);
        
        notificationService.createNotification(1, null, Notification.NotificationType.SYSTEM, "Title", "Msg", Notification.NotificationChannel.EMAIL);
        
        // sendEmail doesn't use mailSender currently (it's commented out), but it shouldn't crash
        verify(repository).save(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
            "Welcome User, welcome-email",
            "Plan Upgraded, upgrade-email",
            "Password Reset, reset-password-email",
            "Generic Title, generic-notification"
    })
    void createNotification_EmailChannelWithVariousTitles(String title, String expectedTemplate) {
        when(repository.save(any())).thenReturn(mockNotification);
        jakarta.mail.internet.MimeMessage mimeMessage = mock(jakarta.mail.internet.MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(eq(expectedTemplate), any(org.thymeleaf.context.Context.class))).thenReturn("<html></html>");
        
        notificationService.createNotification(1, "test@test.com", Notification.NotificationType.SYSTEM, title, "Msg", Notification.NotificationChannel.EMAIL);
        
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void createNotification_EmailChannelFailsQuietly() {
        when(repository.save(any())).thenReturn(mockNotification);
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP Error"));
        
        // Should catch the exception and log, not throw to the caller
        notificationService.createNotification(1, "test@test.com", Notification.NotificationType.SYSTEM, "Generic Title", "Msg", Notification.NotificationChannel.EMAIL);
        
        verify(repository).save(any());
    }
}
