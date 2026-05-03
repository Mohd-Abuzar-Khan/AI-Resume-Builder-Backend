package com.resumade.auth.controller;

import com.resumade.auth.dto.*;
import com.resumade.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login with email and password")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Login with Google")
    @ApiResponse(responseCode = "200", description = "Google login successful")
    @ApiResponse(responseCode = "401", description = "Invalid Google token")
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        log.info("Google login request received");
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout current user")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Refresh JWT token")
    @ApiResponse(responseCode = "200", description = "Token refreshed")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        AuthResponse response = authService.refreshToken(authHeader);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user profile by ID")
    @ApiResponse(responseCode = "200", description = "Profile retrieved")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/profile/{id}")
    public ResponseEntity<AuthResponse> getProfile(@PathVariable("id") Integer id) {
        AuthResponse response = authService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user profile")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @PutMapping("/profile/{id}")
    public ResponseEntity<AuthResponse> updateProfile(
            @PathVariable("id") Integer id,
            @Valid @RequestBody UpdateProfileRequest request) {
        AuthResponse response = authService.updateProfile(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change user password")
    @ApiResponse(responseCode = "200", description = "Password changed")
    @PutMapping("/password/{id}")
    public ResponseEntity<Void> changePassword(
            @PathVariable("id") Integer id,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update subscription plan")
    @ApiResponse(responseCode = "200", description = "Subscription updated")
    @PutMapping("/subscription/{id}")
    public ResponseEntity<Void> updateSubscription(
            @PathVariable("id") Integer id,
            @RequestParam String plan) {
        authService.updateSubscription(id, plan);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Deactivate user account")
    @ApiResponse(responseCode = "200", description = "User deactivated")
    @DeleteMapping("/deactivate/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable("id") Integer id) {
        authService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/deduct-credits/{id}")
    public ResponseEntity<Void> deductCredits(
            @PathVariable("id") Integer id,
            @RequestParam(defaultValue = "5") Integer amount) {
        authService.deductCredits(id, amount);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
