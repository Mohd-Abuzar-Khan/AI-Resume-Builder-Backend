package com.resumade.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumade.auth.dto.*;
import com.resumade.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse = new AuthResponse("jwt-token-123", 1, "John Doe", "john@example.com", "USER", "FREE", 50);
    }

    // ─── Register ───

    @Test
    void register_shouldReturnCreatedStatus() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("john@example.com", response.getBody().getEmail());
        verify(authService).register(request);
    }

    @Test
    void register_shouldPassRequestToService() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPassword("securePass1");
        request.setConfirmPassword("securePass1");

        when(authService.register(any())).thenReturn(mockAuthResponse);

        authController.register(request);

        verify(authService, times(1)).register(request);
    }

    // ─── Login ───

    @Test
    void login_shouldReturnOkWithAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token-123", response.getBody().getToken());
        verify(authService).login(request);
    }

    // ─── Google Login ───

    @Test
    void googleLogin_shouldReturnOkWithAuthResponse() {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setToken("google-id-token");

        when(authService.googleLogin(any(GoogleLoginRequest.class))).thenReturn(mockAuthResponse);

        ResponseEntity<AuthResponse> response = authController.googleLogin(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authService).googleLogin(request);
    }

    // ─── Logout ───

    @Test
    void logout_shouldReturnOk() {
        String authHeader = "Bearer jwt-token-123";

        ResponseEntity<Void> response = authController.logout(authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).logout(authHeader);
    }

    // ─── Refresh Token ───

    @Test
    void refreshToken_shouldReturnNewAuthResponse() {
        String authHeader = "Bearer old-token";
        AuthResponse refreshed = new AuthResponse("new-jwt-token", 1, "John Doe", "john@example.com", "USER", "FREE", 50);
        when(authService.refreshToken(authHeader)).thenReturn(refreshed);

        ResponseEntity<AuthResponse> response = authController.refreshToken(authHeader);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("new-jwt-token", response.getBody().getToken());
    }

    // ─── Get Profile ───

    @Test
    void getProfile_shouldReturnUserProfile() {
        when(authService.getUserById(1)).thenReturn(mockAuthResponse);

        ResponseEntity<AuthResponse> response = authController.getProfile(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("John Doe", response.getBody().getFullName());
    }

    // ─── Update Profile ───

    @Test
    void updateProfile_shouldReturnUpdatedProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("John Updated");

        AuthResponse updated = new AuthResponse(null, 1, "John Updated", "john@example.com", "USER", "FREE", 50);
        when(authService.updateProfile(eq(1), any(UpdateProfileRequest.class))).thenReturn(updated);

        ResponseEntity<AuthResponse> response = authController.updateProfile(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("John Updated", response.getBody().getFullName());
    }

    // ─── Change Password ───

    @Test
    void changePassword_shouldReturnOk() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        ResponseEntity<Void> response = authController.changePassword(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).changePassword(eq(1), any(ChangePasswordRequest.class));
    }

    // ─── Update Subscription ───

    @Test
    void updateSubscription_shouldReturnOk() {
        ResponseEntity<Void> response = authController.updateSubscription(1, "PREMIUM");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).updateSubscription(1, "PREMIUM");
    }

    // ─── Deactivate User ───

    @Test
    void deactivateUser_shouldReturnOk() {
        ResponseEntity<Void> response = authController.deactivateUser(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).deactivateUser(1);
    }

    // ─── Deduct Credits ───

    @Test
    void deductCredits_shouldReturnOkWithDefaultAmount() {
        ResponseEntity<Void> response = authController.deductCredits(1, 5);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).deductCredits(1, 5);
    }

    @Test
    void deductCredits_shouldAcceptCustomAmount() {
        ResponseEntity<Void> response = authController.deductCredits(1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).deductCredits(1, 10);
    }
}
