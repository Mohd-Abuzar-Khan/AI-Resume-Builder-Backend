package com.resumade.auth.service;

import com.resumade.auth.dto.*;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse googleLogin(GoogleLoginRequest request);

    void logout(String token);

    AuthResponse refreshToken(String token);

    AuthResponse getUserById(Integer userId);

    AuthResponse updateProfile(Integer userId, UpdateProfileRequest request);

    void changePassword(Integer userId, ChangePasswordRequest request);

    void updateSubscription(Integer userId, String plan);

    void deactivateUser(Integer userId);

    void deductCredits(Integer userId, Integer amount);

    // Admin methods
    java.util.List<com.resumade.auth.entity.User> getAllUsers();

    java.util.Map<String, Object> getAdminStats();

    void setUserStatus(Integer userId, boolean active);

    void deleteUser(Integer userId);

    void forgotPassword(String email);

    void resetPassword(ResetPasswordRequest request);
}
