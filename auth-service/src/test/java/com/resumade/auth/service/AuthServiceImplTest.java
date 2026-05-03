package com.resumade.auth.service;

import com.resumade.auth.dto.*;
import com.resumade.auth.entity.User;
import com.resumade.auth.exception.EmailAlreadyExistsException;
import com.resumade.auth.exception.InvalidCredentialsException;
import com.resumade.auth.exception.UserNotFoundException;
import com.resumade.auth.repository.UserRepository;
import com.resumade.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private NotificationProducer notificationProducer;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "hashedPass",
                User.Role.USER, User.Provider.LOCAL, true, User.SubscriptionPlan.FREE);
        testUser.setUserId(1);
        testUser.setCredits(50);
    }

    // ─── Register ───

    @Test
    void register_shouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails, testUser)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("john@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("pass");
        request.setConfirmPassword("pass");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    }

    @Test
    void register_shouldThrowWhenPasswordsMismatch() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("pass1");
        request.setConfirmPassword("pass2");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.register(request));
    }

    @Test
    void register_shouldCreateAdminWhenRoleIsAdmin() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Admin User");
        request.setEmail("admin@example.com");
        request.setPassword("adminPass1");
        request.setConfirmPassword("adminPass1");
        request.setRole("ADMIN");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(2);
            return u;
        });
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(jwtService.generateToken(any(), any())).thenReturn("admin-jwt");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository).save(argThat(user -> user.getRole() == User.Role.ADMIN));
    }

    // ─── Login ───

    @Test
    void login_shouldAuthenticateAndReturnToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails, testUser)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_shouldThrowOnInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongPass");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_shouldThrowWhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("pass");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.login(request));
    }

    // ─── Logout ───

    @Test
    void logout_shouldCompleteWithoutException() {
        assertDoesNotThrow(() -> authService.logout("Bearer token"));
    }

    // ─── Refresh Token ───

    @Test
    void refreshToken_shouldReturnNewToken() {
        when(jwtService.extractUsername("old-token")).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtService.validateToken("old-token", userDetails)).thenReturn(true);
        when(jwtService.generateToken(userDetails, testUser)).thenReturn("new-token");

        AuthResponse response = authService.refreshToken("Bearer old-token");

        assertEquals("new-token", response.getToken());
    }

    @Test
    void refreshToken_shouldThrowOnMissingBearer() {
        assertThrows(InvalidCredentialsException.class, () -> authService.refreshToken("InvalidHeader"));
    }

    @Test
    void refreshToken_shouldThrowOnNullHeader() {
        assertThrows(InvalidCredentialsException.class, () -> authService.refreshToken(null));
    }

    @Test
    void refreshToken_shouldThrowOnInvalidToken() {
        when(jwtService.extractUsername("expired")).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername("john@example.com")).thenReturn(userDetails);
        when(jwtService.validateToken("expired", userDetails)).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.refreshToken("Bearer expired"));
    }

    // ─── Get User By ID ───

    @Test
    void getUserById_shouldReturnUserProfile() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        AuthResponse response = authService.getUserById(1);

        assertEquals("John Doe", response.getFullName());
        assertNull(response.getToken());
    }

    @Test
    void getUserById_shouldThrowWhenNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.getUserById(999));
    }

    // ─── Update Profile ───

    @Test
    void updateProfile_shouldUpdateAllFields() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("John Updated");
        request.setEmail("updated@example.com");
        request.setPhone("+1234567890");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.updateProfile(1, request);

        verify(userRepository).save(argThat(u ->
                "John Updated".equals(u.getFullName()) &&
                "updated@example.com".equals(u.getEmail()) &&
                "+1234567890".equals(u.getPhone())
        ));
    }

    @Test
    void updateProfile_shouldNotUpdateNullFields() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.updateProfile(1, request);

        verify(userRepository).save(argThat(u -> "john@example.com".equals(u.getEmail())));
    }

    // ─── Change Password ───

    @Test
    void changePassword_shouldUpdatePassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "hashedPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("newHash");

        authService.changePassword(1, request);

        verify(userRepository).save(argThat(u -> "newHash".equals(u.getPasswordHash())));
    }

    @Test
    void changePassword_shouldThrowOnWrongCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongOld");
        request.setNewPassword("new");
        request.setConfirmPassword("new");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOld", "hashedPass")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(1, request));
    }

    @Test
    void changePassword_shouldThrowOnPasswordMismatch() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("new1");
        request.setConfirmPassword("new2");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPass", "hashedPass")).thenReturn(true);

        assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(1, request));
    }

    // ─── Update Subscription ───

    @Test
    void updateSubscription_shouldUpgradeToPremium() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.updateSubscription(1, "PREMIUM");

        verify(userRepository).save(argThat(u ->
                u.getSubscriptionPlan() == User.SubscriptionPlan.PREMIUM && u.getCredits() == 1000
        ));
    }

    @Test
    void updateSubscription_shouldKeepCreditsForFree() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.updateSubscription(1, "FREE");

        verify(userRepository).save(argThat(u -> u.getSubscriptionPlan() == User.SubscriptionPlan.FREE));
    }

    @Test
    void updateSubscription_shouldHandleNotificationFailure() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doThrow(new RuntimeException("RabbitMQ down")).when(notificationProducer).sendNotification(any());

        assertDoesNotThrow(() -> authService.updateSubscription(1, "PREMIUM"));
    }

    // ─── Deduct Credits ───

    @Test
    void deductCredits_shouldBypassCreditDeduction() {
        assertDoesNotThrow(() -> authService.deductCredits(1, 5));
    }

    // ─── Deactivate User ───

    @Test
    void deactivateUser_shouldSetUserInactive() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.deactivateUser(1);

        verify(userRepository).save(argThat(u -> !u.getIsActive()));
    }

    // ─── Admin Methods ───

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = authService.getAllUsers();

        assertEquals(1, result.size());
    }

    @Test
    void getAdminStats_shouldReturnCorrectStats() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countBySubscriptionPlan(User.SubscriptionPlan.PREMIUM)).thenReturn(25L);
        when(userRepository.countByIsActiveTrue()).thenReturn(90L);

        Map<String, Object> stats = authService.getAdminStats();

        assertEquals(100L, stats.get("totalUsers"));
        assertEquals(25L, stats.get("premiumUsers"));
        assertEquals(90L, stats.get("activeUsers"));
    }

    @Test
    void setUserStatus_shouldUpdateActiveStatus() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.setUserStatus(1, true);

        verify(userRepository).save(argThat(User::getIsActive));
    }

    @Test
    void deleteUser_shouldDeleteById() {
        authService.deleteUser(1);

        verify(userRepository).deleteById(1);
    }
}
