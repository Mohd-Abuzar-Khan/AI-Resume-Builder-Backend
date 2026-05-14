package com.resumade.auth.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.resumade.auth.dto.AuthResponse;
import com.resumade.auth.dto.ChangePasswordRequest;
import com.resumade.auth.dto.GoogleLoginRequest;
import com.resumade.auth.dto.LoginRequest;
import com.resumade.auth.dto.RegisterRequest;
import com.resumade.auth.dto.ResetPasswordRequest;
import com.resumade.auth.dto.UpdateProfileRequest;
import com.resumade.auth.entity.User;
import com.resumade.auth.exception.EmailAlreadyExistsException;
import com.resumade.auth.exception.InvalidCredentialsException;
import com.resumade.auth.exception.UserNotFoundException;
import com.resumade.auth.repository.UserRepository;
import com.resumade.auth.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private NotificationProducer notificationProducer;
    @Mock private UserDetails userDetails;
    @Mock private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;
    @Mock private org.springframework.data.redis.core.ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "hashedPass",
                User.Role.USER, User.Provider.LOCAL, true, User.SubscriptionPlan.FREE, null);
        testUser.setUserId(1);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
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

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

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

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

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
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

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
    void logout_shouldRemoveTokenFromRedis() {
        when(jwtService.extractUsername("token")).thenReturn("john@example.com");

        authService.logout("Bearer token");

        verify(redisTemplate).delete("JWT_TOKEN:john@example.com");
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
                u.getSubscriptionPlan() == User.SubscriptionPlan.PREMIUM
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

    // ─── Password Reset ───

    @Test
    void forgotPassword_shouldSaveTokenAndNotify() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.forgotPassword("john@example.com");

        verify(userRepository).save(argThat(u -> u.getResetToken() != null && u.getResetTokenExpiry() != null));
        verify(notificationProducer).sendNotification(any());
    }

    @Test
    void forgotPassword_shouldNotThrowOnNotificationFailure() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doThrow(new RuntimeException("down")).when(notificationProducer).sendNotification(any());

        assertDoesNotThrow(() -> authService.forgotPassword("john@example.com"));
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndClearToken() {
        User user = new User("Reset User", "reset@example.com", "oldHash",
                User.Role.USER, User.Provider.LOCAL, true, User.SubscriptionPlan.FREE, null);
        user.setResetToken("reset-token");
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        when(userRepository.findByResetToken("reset-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPass123")).thenReturn("newHash");

        authService.resetPassword(request);

        verify(userRepository).save(argThat(u ->
                "newHash".equals(u.getPasswordHash()) &&
                u.getResetToken() == null &&
                u.getResetTokenExpiry() == null));
    }

    @Test
    void resetPassword_shouldThrowWhenTokenExpired() {
        User user = new User("Reset User", "reset@example.com", "oldHash",
                User.Role.USER, User.Provider.LOCAL, true, User.SubscriptionPlan.FREE, null);
        user.setResetToken("reset-token");
        user.setResetTokenExpiry(LocalDateTime.now().minusMinutes(1));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        when(userRepository.findByResetToken("reset-token")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.resetPassword(request));
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

    // ─── Google Login ───

    @Test
    void googleLogin_shouldThrowWhenTokenMissing() {
        GoogleLoginRequest request = new GoogleLoginRequest("");

        assertThrows(InvalidCredentialsException.class, () -> authService.googleLogin(request));
    }

    @Test
    void googleLogin_shouldThrowWhenTokenInvalid() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("bad-token");
        AuthServiceImpl spy = org.mockito.Mockito.spy(authService);

        doReturn(null).when(spy).verifyGoogleToken("bad-token");

        assertThrows(InvalidCredentialsException.class, () -> spy.googleLogin(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void googleLogin_shouldRegisterNewUserAndReturnAuthResponse() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("valid-token");
        AuthServiceImpl spy = org.mockito.Mockito.spy(authService);
        ReflectionTestUtils.setField(spy, "googleClientId", "client-id");

        GoogleIdToken.Payload payload = org.mockito.Mockito.mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn("new@example.com");
        when(payload.get("name")).thenReturn("New User");
        when(payload.getIssuer()).thenReturn("issuer");
        when(payload.get("aud")).thenReturn("client-id");

        GoogleIdToken idToken = org.mockito.Mockito.mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);

        doReturn(idToken).when(spy).verifyGoogleToken("valid-token");

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(10);
            return u;
        });
        when(userDetailsService.loadUserByUsername("new@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(eq(userDetails), any(User.class))).thenReturn("jwt-token");

        AuthResponse response = spy.googleLogin(request);

        assertEquals("jwt-token", response.getToken());
        verify(userRepository).save(argThat(u -> u.getProvider() == User.Provider.GOOGLE));
        verify(valueOperations).set(eq("JWT_TOKEN:new@example.com"), eq("jwt-token"), any(java.time.Duration.class));
    }

    @Test
    void googleLogin_shouldUpdateProviderWhenMissing() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("valid-token");
        AuthServiceImpl spy = org.mockito.Mockito.spy(authService);
        ReflectionTestUtils.setField(spy, "googleClientId", "client-id");

        User existing = new User("Existing", "exist@example.com", "hash",
                User.Role.USER, null, true, User.SubscriptionPlan.FREE, null);
        existing.setUserId(5);
        existing.setProvider(null);

        GoogleIdToken.Payload payload = org.mockito.Mockito.mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn("exist@example.com");
        when(payload.get("name")).thenReturn("Existing");
        when(payload.getIssuer()).thenReturn("issuer");
        when(payload.get("aud")).thenReturn("client-id");

        GoogleIdToken idToken = org.mockito.Mockito.mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);

        doReturn(idToken).when(spy).verifyGoogleToken("valid-token");

        when(userRepository.findByEmail("exist@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userDetailsService.loadUserByUsername("exist@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(eq(userDetails), any(User.class))).thenReturn("jwt-token");

        AuthResponse response = spy.googleLogin(request);

        assertEquals("jwt-token", response.getToken());
        verify(userRepository).save(argThat(u -> u.getProvider() == User.Provider.GOOGLE));
    }

    @Test
    void googleLogin_shouldCatchIOExceptionFromVerifier() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("valid-token");
        AuthServiceImpl spy = org.mockito.Mockito.spy(authService);
        
        doThrow(new java.io.IOException("Network error"))
            .when(spy).verifyGoogleToken("valid-token");

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> spy.googleLogin(request));
        assertEquals("Failed to verify Google token: Network error", ex.getMessage());
    }

    @Test
    void googleLogin_shouldCatchGeneralSecurityExceptionFromVerifier() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest("valid-token");
        AuthServiceImpl spy = org.mockito.Mockito.spy(authService);
        
        doThrow(new java.security.GeneralSecurityException("Security error"))
            .when(spy).verifyGoogleToken("valid-token");

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> spy.googleLogin(request));
        assertEquals("Failed to verify Google token: Security error", ex.getMessage());
    }

    @Test
    void verifyGoogleToken_shouldCatchGoogleJsonResponseException() {
        AuthServiceImpl spy = org.mockito.Mockito.spy(authService);
        ReflectionTestUtils.setField(spy, "googleClientId", "client-id");
        
        try (org.mockito.MockedConstruction<com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier> mocked = 
             org.mockito.Mockito.mockConstruction(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class, 
             (mock, context) -> {
                 com.google.api.client.googleapis.json.GoogleJsonResponseException mockEx = 
                     org.mockito.Mockito.mock(com.google.api.client.googleapis.json.GoogleJsonResponseException.class);
                 when(mock.verify(anyString())).thenThrow(mockEx);
             })) {
            
            assertThrows(java.io.IOException.class, () -> spy.verifyGoogleToken("token"));
        }
    }

    @Test
    void verifyGoogleToken_shouldCatchIOException() {
        AuthServiceImpl spy = org.mockito.Mockito.spy(authService);
        ReflectionTestUtils.setField(spy, "googleClientId", "client-id");
        
        try (org.mockito.MockedConstruction<com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier> mocked = 
             org.mockito.Mockito.mockConstruction(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class, 
             (mock, context) -> {
                 when(mock.verify(anyString())).thenThrow(new java.io.IOException("Network error"));
             })) {
            
            assertThrows(java.io.IOException.class, () -> spy.verifyGoogleToken("token"));
        }
    }

    @Test
    void verifyGoogleToken_shouldCatchGeneralSecurityException() {
        AuthServiceImpl spy = org.mockito.Mockito.spy(authService);
        ReflectionTestUtils.setField(spy, "googleClientId", "client-id");
        
        try (org.mockito.MockedConstruction<com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier> mocked = 
             org.mockito.Mockito.mockConstruction(com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.class, 
             (mock, context) -> {
                 when(mock.verify(anyString())).thenThrow(new java.security.GeneralSecurityException("Security error"));
             })) {
            
            assertThrows(java.security.GeneralSecurityException.class, () -> spy.verifyGoogleToken("token"));
        }
    }
}
