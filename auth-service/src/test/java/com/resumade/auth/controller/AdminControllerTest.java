package com.resumade.auth.controller;

import com.resumade.auth.entity.User;
import com.resumade.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AdminController adminController;

    private List<User> mockUsers;

    @BeforeEach
    void setUp() {
        User user1 = new User("John", "john@test.com", "hash", User.Role.USER, User.Provider.LOCAL, true, User.SubscriptionPlan.FREE, null);
        user1.setUserId(1);
        User user2 = new User("Jane", "jane@test.com", "hash", User.Role.ADMIN, User.Provider.GOOGLE, true, User.SubscriptionPlan.PREMIUM, null);
        user2.setUserId(2);
        mockUsers = Arrays.asList(user1, user2);
    }

    @Test
    void getAllUsers_shouldReturnUserList() {
        when(authService.getAllUsers()).thenReturn(mockUsers);

        ResponseEntity<List<User>> response = adminController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(authService).getAllUsers();
    }

    @Test
    void getAdminStats_shouldReturnStatsMap() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 100L);
        stats.put("premiumUsers", 25L);
        stats.put("activeUsers", 90L);
        when(authService.getAdminStats()).thenReturn(stats);

        ResponseEntity<Map<String, Object>> response = adminController.getAdminStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(100L, response.getBody().get("totalUsers"));
        assertEquals(25L, response.getBody().get("premiumUsers"));
    }

    @Test
    void setUserStatus_shouldCallServiceAndReturnOk() {
        ResponseEntity<Void> response = adminController.setUserStatus(1, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).setUserStatus(1, true);
    }

    @Test
    void setUserStatus_shouldDeactivateUser() {
        ResponseEntity<Void> response = adminController.setUserStatus(1, false);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).setUserStatus(1, false);
    }

    @Test
    void updateUserPlan_shouldCallServiceAndReturnOk() {
        ResponseEntity<Void> response = adminController.updateUserPlan(1, "PREMIUM");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).updateSubscription(1, "PREMIUM");
    }

    @Test
    void deleteUser_shouldCallServiceAndReturnNoContent() {
        ResponseEntity<Void> response = adminController.deleteUser(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(authService).deleteUser(1);
    }
}
