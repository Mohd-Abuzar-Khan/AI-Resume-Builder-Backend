package com.resumade.auth.service;

import com.resumade.auth.dto.*;
import com.resumade.auth.entity.User;
import com.resumade.auth.exception.EmailAlreadyExistsException;
import com.resumade.auth.exception.InvalidCredentialsException;
import com.resumade.auth.exception.UserNotFoundException;

import com.resumade.auth.repository.UserRepository;
import com.resumade.auth.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.Optional;

@Service
// Central authentication orchestrator — handles local/Google auth, JWT lifecycle, and user management
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final NotificationProducer notificationProducer;
    // Redis stores active JWT tokens to enable server-side session invalidation on logout
    private final org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;
    private final com.resumade.auth.repository.PaymentRecordRepository paymentRecordRepository;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, AuthenticationManager authenticationManager,
            UserDetailsService userDetailsService, NotificationProducer notificationProducer,
            org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate,
            com.resumade.auth.repository.PaymentRecordRepository paymentRecordRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.notificationProducer = notificationProducer;
        this.redisTemplate = redisTemplate;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    // Registers a new user, sends welcome notification via RabbitMQ, and returns JWT
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("An account with this email already exists");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        // WARNING: Role from request is trusted — allows self-assignment of ADMIN (see audit)
        User.Role role = User.Role.USER;
        if (request.getRole() != null && request.getRole().equalsIgnoreCase("ADMIN")) {
            role = User.Role.ADMIN;
        }

        User user = new User(
                request.getFullName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                role,
                User.Provider.LOCAL,
                true,
                User.SubscriptionPlan.FREE,
                request.getProfilePicture());

        user = userRepository.save(user);
        log.info("User registered successfully with id: {}", user.getUserId());

        // Send registration notification
        try {
            notificationProducer.sendNotification(new NotificationEvent(
                    user.getUserId(),
                    user.getEmail(),
                    "SYSTEM",
                    "Welcome to Resumade!",
                    "Thank you for registering. We're excited to have you on board!",
                    "BOTH"));
        } catch (Exception e) {
            log.error("Failed to send welcome notification: {}", e.getMessage());
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, user);

        // Store token in Redis
        saveTokenToRedis(user.getEmail(), token);

        return buildAuthResponse(token, user);
    }

    // Authenticates via Spring Security's AuthenticationManager, then issues a new JWT
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails, user);

        // Store token in Redis
        saveTokenToRedis(user.getEmail(), token);

        log.info("User logged in successfully: {}", user.getUserId());

        // Send login notification
        try {
            notificationProducer.sendNotification(new NotificationEvent(
                    user.getUserId(),
                    user.getEmail(),
                    "AUTH",
                    "Security Alert: New Login",
                    "A new login was detected for your Resumade account on " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ". If this wasn't you, please change your password immediately.",
                    "EMAIL"));
        } catch (Exception e) {
            log.error("Failed to send login notification: {}", e.getMessage());
        }

        return buildAuthResponse(token, user);
    }

    // Verifies Google ID token, auto-registers new users, and issues internal JWT
    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        log.info("Google login attempt with token");

        if (request.getToken() == null || request.getToken().isBlank()) {
            log.warn("Google login attempt with empty token");
            throw new InvalidCredentialsException("Google token is required");
        }

        log.debug("Token length: {} chars", request.getToken().length());
        log.debug("Configured Google Client ID: {}", googleClientId);

        try {
            GoogleIdToken idToken = verifyGoogleToken(request.getToken());
            if (idToken == null) {
                log.warn("Google token verification returned null - token may be invalid or expired");
                throw new InvalidCredentialsException("Invalid Google token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            if (name == null) {
                name = email; // Use email as fallback if name not provided
            }
            String tokenIssuer = payload.getIssuer();
            Object audienceObj = payload.get("aud");
            String tokenAudience = audienceObj != null ? audienceObj.toString() : "unknown";

            log.debug("Token issuer: {}", tokenIssuer);
            log.debug("Token audience: {}", tokenAudience);
            log.debug("Token verified for email: {}", email);

            if (email == null || email.isBlank()) {
                log.warn("Google token missing email claim");
                throw new InvalidCredentialsException("Google token missing required email claim");
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            User user;

            if (userOpt.isPresent()) {
                user = userOpt.get();
                log.info("Existing user found for Google login: {}", user.getUserId());
                // Prevents provider hijacking — don't overwrite LOCAL with GOOGLE for existing accounts
                if (user.getProvider() == null) {
                    log.info("Setting provider to GOOGLE for existing user: {}", user.getUserId());
                    user.setProvider(User.Provider.GOOGLE);
                    userRepository.save(user);
                }
            } else {
                // Register new user via Google
                log.info("Creating new user via Google with email: {}", email);
                user = new User(
                        name,
                        email,
                        null, // No password for Google users
                        User.Role.USER,
                        User.Provider.GOOGLE,
                        true,
                        User.SubscriptionPlan.FREE,
                        null);
                user = userRepository.save(user);
                log.info("New user registered via Google: {} with email: {}", user.getUserId(), email);
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String jwtToken = jwtService.generateToken(userDetails, user);

            // Store token in Redis
            saveTokenToRedis(user.getEmail(), jwtToken);

            log.info("Google login successful for user: {}", user.getUserId());

            // Send login notification
            try {
                notificationProducer.sendNotification(new NotificationEvent(
                        user.getUserId(),
                        user.getEmail(),
                        "AUTH",
                        "Security Alert: Google Login",
                        "A new login via Google was detected for your Resumade account on " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ".",
                        "EMAIL"));
            } catch (Exception e) {
                log.error("Failed to send Google login notification: {}", e.getMessage());
            }

            return buildAuthResponse(jwtToken, user);

        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (java.io.IOException | java.security.GeneralSecurityException e) {
            throw new InvalidCredentialsException("Failed to verify Google token: " + e.getMessage(), e);
        }
    }

    // Protected visibility allows test subclasses to mock Google token verification
    protected GoogleIdToken verifyGoogleToken(String token) throws java.io.IOException, java.security.GeneralSecurityException {
        log.debug("Starting token verification with client ID: {}", googleClientId);

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            log.debug("Verifier created, attempting to verify token of length: {}", token.length());
            GoogleIdToken idToken = verifier.verify(token);
            log.debug("Token verification result: {}", idToken != null ? "SUCCESS" : "NULL");
            return idToken;
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            throw new java.io.IOException("Google token invalid", e);
        } catch (java.io.IOException e) {
            throw new java.io.IOException("Cannot reach Google API to verify token. Check backend internet connectivity.", e);
        } catch (java.security.GeneralSecurityException e) {
            throw new java.security.GeneralSecurityException("Token verification exception", e);
        }
    }

    // Invalidates session by removing JWT from Redis — subsequent requests with this token will fail
    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            String email = jwtService.extractUsername(jwt);
            redisTemplate.delete("JWT_TOKEN:" + email);
            log.info("User logged out and token removed from Redis for: {}", email);
        }
    }

    // Issues a new JWT if the old one is still valid — does NOT invalidate the old token in Redis
    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Invalid token");
        }

        String oldToken = authHeader.substring(7);
        String email = jwtService.extractUsername(oldToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.validateToken(oldToken, userDetails)) {
            throw new InvalidCredentialsException("Invalid or expired token");
        }

        String newToken = jwtService.generateToken(userDetails, user);
        return buildAuthResponse(newToken, user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return buildAuthResponse(null, user);
    }

    @Override
    @Transactional
    public AuthResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setFullName(request.getFullName());
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getProfilePicture() != null) {
            user.setProfilePicture(request.getProfilePicture());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);
        return buildAuthResponse(null, user);
    }

    @Override
    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("New passwords do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", userId);
    }

    // Upgrades user plan and resets credit balance — called by PaymentService after successful payment
    @Override
    @Transactional
    public void updateSubscription(Integer userId, String plan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        User.SubscriptionPlan newPlan = User.SubscriptionPlan.valueOf(plan.toUpperCase());
        user.setSubscriptionPlan(newPlan);



        userRepository.save(user);
        log.info("Subscription updated to {} for user: {}.", plan, userId);

        // Send notification
        try {
            notificationProducer.sendNotification(new NotificationEvent(
                    userId,
                    user.getEmail(),
                    "SYSTEM",
                    "Plan Upgraded!",
                    "Your account has been upgraded to " + plan.toUpperCase() + ".",
                    "BOTH"));
        } catch (Exception e) {
            log.error("Failed to send plan update notification: {}", e.getMessage());
        }
    }



    @Override
    @Transactional
    public void deactivateUser(Integer userId) {
        setUserStatus(userId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getAdminStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("premiumUsers", userRepository.countBySubscriptionPlan(User.SubscriptionPlan.PREMIUM));
        stats.put("activeUsers", userRepository.countByIsActiveTrue());
        return stats;
    }

    @Override
    @Transactional
    public void setUserStatus(Integer userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setIsActive(active);
        userRepository.save(user);
        log.info("Status updated for user {}: {}", userId, active);
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        userRepository.deleteById(userId);
        log.info("User deleted: {}", userId);
    }

    // Generates a 6-digit OTP with 15-minute expiry and sends it via notification queue
    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setResetToken(otp);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        try {
            notificationProducer.sendNotification(new NotificationEvent(
                    user.getUserId(),
                    user.getEmail(),
                    "AUTH",
                    "Password Reset OTP",
                    "Your OTP for password reset is: " + otp + ". It will expire in 15 minutes.",
                    "EMAIL"));
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        if (user.getResetToken() == null || !user.getResetToken().equals(request.getToken())) {
            throw new InvalidCredentialsException("Invalid reset token or OTP");
        }

        if (user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new InvalidCredentialsException("Reset token or OTP has expired");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidCredentialsException("Passwords do not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password reset successfully for user: {}", user.getUserId());
    }

    // Persists JWT in Redis with TTL to enable server-side logout and single-session enforcement
    private void saveTokenToRedis(String email, String token) {
        redisTemplate.opsForValue().set(
            "JWT_TOKEN:" + email, 
            token, 
            java.time.Duration.ofMillis(86400000) // 24 hours — should match jwt.expiration config value
        );
    }

    // Maps entity to API response — token is null for profile-only responses (getUserById)
    private AuthResponse buildAuthResponse(String token, User user) {
        return new AuthResponse(
                token,
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getSubscriptionPlan().name(),
                user.getCredits(),
                user.getProfilePicture());
    }
    @Override
    @Transactional
    public void promoteToAdmin(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        user.setRole(User.Role.ADMIN);
        userRepository.save(user);
    }

    @Override
    public java.util.List<com.resumade.auth.dto.AdminPaymentDTO> getAllPayments() {
        return paymentRecordRepository.findAll().stream().map(payment -> {
            User user = userRepository.findById(payment.getUserId()).orElse(null);
            String fullName = (user != null) ? user.getFullName() : "Unknown User";
            String plan = (user != null) ? user.getSubscriptionPlan().name() : "Unknown";
            String status = payment.getStatus();
            if ("SUCCESS".equals(status) || "Paid".equalsIgnoreCase(status)) status = "Paid";
            else if ("CREATED".equals(status) || "Pending".equalsIgnoreCase(status)) status = "Pending";
            else status = "Failed";
            
            String date = payment.getCreatedAt() != null ? payment.getCreatedAt().toLocalDate().toString() : java.time.LocalDate.now().toString();

            return new com.resumade.auth.dto.AdminPaymentDTO(
                    date,
                    fullName,
                    plan.substring(0, 1).toUpperCase() + plan.substring(1).toLowerCase(),
                    payment.getAmount(),
                    status
            );
        }).collect(java.util.stream.Collectors.toList());
    }
}
