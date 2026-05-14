# Auth Service Code Guide

This document walks through every file in the auth-service and explains the main code blocks inside each one.

## Service summary

The auth-service handles:
- User registration, login, Google OAuth login, and JWT issuance.
- JWT validation in requests and token revocation via Redis.
- Profile management, password changes, password reset flow.
- Admin operations (stats, user management).
- Payment flow via Razorpay and subscription upgrades.
- Notification publishing via RabbitMQ.

## Bootstrapping and configuration

### src/main/java/com/resumade/auth/AuthServiceApplication.java

Purpose: Spring Boot entry point with OpenAPI metadata and Eureka registration.

Main blocks:
- OpenAPI definition: sets title, version, description for Swagger UI.
- @EnableDiscoveryClient: registers the service with Eureka.
- main method: starts the Spring Boot application.

### src/main/java/com/resumade/auth/config/SecurityConfig.java

Purpose: Spring Security configuration, including JWT filter and authentication provider.

Main blocks:
- Bean SecurityFilterChain: disables CSRF, declares public endpoints, requires auth for the rest, uses stateless sessions, and registers the JWT filter.
- AuthenticationProvider: DaoAuthenticationProvider with CustomUserDetailsService and BCrypt.
- AuthenticationManager: exposed from AuthenticationConfiguration.
- PasswordEncoder: BCryptPasswordEncoder.

### src/main/java/com/resumade/auth/config/RedisConfig.java

Purpose: RedisTemplate configuration for storing JWTs by email.

Main blocks:
- RedisTemplate bean: uses String serializers for keys and values.

### src/main/java/com/resumade/auth/config/RabbitConfig.java

Purpose: RabbitMQ config for JSON messages.

Main blocks:
- Jackson2JsonMessageConverter bean for payload serialization.
- RabbitTemplate bean using the JSON converter.

## Security

### src/main/java/com/resumade/auth/security/CustomUserDetailsService.java

Purpose: Loads user credentials and roles for Spring Security.

Main blocks:
- loadUserByUsername: fetches the user by email, throws if missing, returns a Spring Security User with role and active flag.

### src/main/java/com/resumade/auth/security/JwtService.java

Purpose: Creates and validates JWTs.

Main blocks:
- generateToken: builds JWT with extra claims (userId, role, plan, fullName, credits).
- extractUsername/extractExpiration: reads standard claims.
- extractClaim/extractAllClaims: shared claim parsing logic.
- validateToken: verifies username match and expiration.
- buildToken/getSigningKey: signs with HS256 using configured secret.

### src/main/java/com/resumade/auth/security/JwtAuthFilter.java

Purpose: Validates incoming JWTs and checks Redis for token presence.

Main blocks:
- doFilterInternal: extracts Bearer token, parses username, loads user, verifies token matches Redis value, sets SecurityContext if valid.
- Redis check: ensures only the latest stored token is accepted (logout removes it).

## Controllers

### src/main/java/com/resumade/auth/controller/AuthController.java

Purpose: Public and authenticated endpoints for auth and profile management.

Main blocks:
- /register: validates input, creates user, returns AuthResponse with JWT.
- /login: authenticates credentials, returns AuthResponse with JWT.
- /google: verifies Google token and returns AuthResponse with JWT.
- /logout: removes token from Redis.
- /refresh: validates old token and returns new JWT.
- /profile/{id}: read profile by id.
- /profile/{id} PUT: update profile.
- /password/{id}: change password.
- /subscription/{id}: update plan.
- /deactivate/{id}: deactivate account.
- /deduct-credits/{id}: deduct credits (currently no-op in service).
- /forgot-password: request reset token.
- /reset-password: perform password reset.

### src/main/java/com/resumade/auth/controller/AdminController.java

Purpose: Admin endpoints for user and stats management.

Main blocks:
- /users: list all users.
- /stats: returns total users, premium count, active count.
- /users/{id}/status: toggle active state.
- /users/{id}/plan: update subscription plan.
- /users/{id}: delete user.

### src/main/java/com/resumade/auth/controller/PaymentController.java

Purpose: Payment integration endpoints for Razorpay.

Main blocks:
- /create-order: creates a Razorpay order and stores a PaymentRecord.
- /verify: marks payment successful and upgrades plan.
- /history/{userId}: returns payment history for user.
- /webhook: placeholder for webhook ingestion.

## Services

### src/main/java/com/resumade/auth/service/AuthService.java

Purpose: Interface for all auth-related operations.

Main blocks:
- Authentication: register, login, Google login, logout, refresh.
- Profile operations: get, update, change password.
- Subscription and account: update plan, deactivate.
- Admin: list users, stats, set status, delete.
- Password reset: forgot/reset endpoints.

### src/main/java/com/resumade/auth/service/AuthServiceImpl.java

Purpose: Concrete implementation of authentication and user management logic.

Main blocks:
- register: validates uniqueness and password match, creates user, sends notification, issues JWT, stores token in Redis.
- login: authenticates credentials, issues JWT, stores token in Redis.
- googleLogin: verifies token with Google client, creates or updates user, issues JWT.
- logout: deletes JWT from Redis so it becomes invalid.
- refreshToken: validates old token, issues a new one.
- getUserById/updateProfile: basic user info access.
- changePassword: validates current password and confirm password.
- updateSubscription: updates plan and credits, sends notification.
- deductCredits: disabled (logs only).
- admin methods: stats, list, deactivate, delete.
- forgotPassword/resetPassword: generates reset token, saves expiry, sends reset email, validates on reset.
- saveTokenToRedis/buildAuthResponse: helper methods for token storage and response mapping.

### src/main/java/com/resumade/auth/service/PaymentService.java

Purpose: Encapsulates Razorpay payment logic.

Main blocks:
- createOrder: constructs Razorpay order, stores PaymentRecord, returns order data for client checkout.
- verifyPayment: marks record as SUCCESS and upgrades user to PREMIUM (signature check is placeholder).
- getPaymentHistory: lists all PaymentRecord entries for a user.

### src/main/java/com/resumade/auth/service/NotificationProducer.java

Purpose: Publishes notification events to RabbitMQ.

Main blocks:
- sendNotification: sends NotificationEvent to configured exchange and routing key.

## Entities

### src/main/java/com/resumade/auth/entity/User.java

Purpose: User table model for authentication and profile data.

Main blocks:
- Fields: identity, email, password hash, role, provider, plan, credits, active flag, reset token, timestamps.
- Constructor: sets defaults and initial credits based on plan.
- Enums: Role, Provider, SubscriptionPlan.
- Getters/setters for all fields.

### src/main/java/com/resumade/auth/entity/PaymentRecord.java

Purpose: Records payment attempts and results.

Main blocks:
- Fields: userId, orderId, paymentId, signature, amount, currency, status, timestamps.
- Getters/setters.

## Repositories

### src/main/java/com/resumade/auth/repository/UserRepository.java

Purpose: Database access for User entities.

Main blocks:
- Query helpers: findByEmail, existsByEmail, plan/role filters, counts, findByResetToken.

### src/main/java/com/resumade/auth/repository/PaymentRecordRepository.java

Purpose: Database access for PaymentRecord entities.

Main blocks:
- Query helpers: findByUserId, findByOrderId.

## DTOs

### src/main/java/com/resumade/auth/dto/AuthResponse.java

Purpose: Standard response with JWT and user info.

Main blocks:
- Fields for token, userId, identity, plan, credits.
- Constructors and getters/setters.

### src/main/java/com/resumade/auth/dto/RegisterRequest.java

Purpose: Validation-backed payload for registration.

Main blocks:
- Fields: fullName, email, password, confirmPassword, role.
- Validation: NotBlank, Email, size on password.

### src/main/java/com/resumade/auth/dto/LoginRequest.java

Purpose: Login payload.

Main blocks:
- Fields: email, password.
- Validation: NotBlank, Email.

### src/main/java/com/resumade/auth/dto/GoogleLoginRequest.java

Purpose: Google OAuth login payload.

Main blocks:
- Field: token with NotBlank validation.

### src/main/java/com/resumade/auth/dto/UpdateProfileRequest.java

Purpose: Profile update payload.

Main blocks:
- Fields: fullName, email, phone.
- Validation: NotBlank on fullName, Email for email.

### src/main/java/com/resumade/auth/dto/ChangePasswordRequest.java

Purpose: Password change payload.

Main blocks:
- Fields: currentPassword, newPassword, confirmPassword.
- Validation: NotBlank, size for newPassword.

### src/main/java/com/resumade/auth/dto/ResetPasswordRequest.java

Purpose: Password reset payload.

Main blocks:
- Fields: token, newPassword, confirmPassword.
- Validation: NotBlank, size for newPassword.

### src/main/java/com/resumade/auth/dto/RazorpayOrderRequest.java

Purpose: Payment order creation input.

Main blocks:
- Fields: amount, currency.

### src/main/java/com/resumade/auth/dto/RazorpayOrderResponse.java

Purpose: Payment order creation response.

Main blocks:
- Fields: orderId, currency, amount, key.
- Constructors and getters/setters.

### src/main/java/com/resumade/auth/dto/NotificationEvent.java

Purpose: Message payload published to RabbitMQ.

Main blocks:
- Fields: userId, recipientEmail, type, title, message, channel.
- Constructors and getters/setters.

## Exceptions

### src/main/java/com/resumade/auth/exception/EmailAlreadyExistsException.java

Purpose: Signals a registration conflict for duplicate email.

Main block:
- RuntimeException subclass with message.

### src/main/java/com/resumade/auth/exception/InvalidCredentialsException.java

Purpose: Signals authentication or validation failures.

Main block:
- RuntimeException subclass with message.

### src/main/java/com/resumade/auth/exception/InsufficientCreditsException.java

Purpose: Signals insufficient credits for a paid feature.

Main block:
- RuntimeException subclass with message.

### src/main/java/com/resumade/auth/exception/TokenExpiredException.java

Purpose: Signals expired or invalid token use.

Main block:
- RuntimeException subclass with message.

### src/main/java/com/resumade/auth/exception/UserNotFoundException.java

Purpose: Signals missing user in storage.

Main block:
- RuntimeException subclass with message.

### src/main/java/com/resumade/auth/exception/GlobalExceptionHandler.java

Purpose: Central exception-to-response mapper.

Main blocks:
- Per-exception handlers that map to HTTP status codes.
- Validation handler that aggregates field errors.
- Fallback handler for unexpected exceptions.
- buildResponse helper to generate consistent response bodies.

## Configuration file

### src/main/resources/application.yml

Purpose: Service runtime configuration.

Main blocks:
- server.port: 9091.
- spring.datasource: MySQL config for resumade_auth.
- spring.data.redis: Redis host/port.
- eureka: discovery registration.
- jwt: secret and expiration (24 hours).
- springdoc: OpenAPI paths.
- management: actuator exposure.
- rabbitmq: connection settings (under management key in this file).
- notification: exchange and routing key.
- razorpay: API keys.
- google.oauth: client id, secret, redirect URI.

## Build file

### pom.xml

Purpose: Maven build and dependency graph.

Main blocks:
- Dependencies: web, security, JPA, validation, actuator, Eureka, OpenAPI, JWT, MySQL, Razorpay, Google OAuth, RabbitMQ, Redis.
- Plugin setup: JaCoCo for coverage, Surefire for tests, Spring Boot plugin, Sonar scanner.
