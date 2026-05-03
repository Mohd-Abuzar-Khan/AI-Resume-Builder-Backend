package com.resumade.auth.controller;

import com.resumade.auth.entity.User;
import com.resumade.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/admin")
@Tag(name = "Admin", description = "Admin management endpoints")
public class AdminController {

    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Get all users")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @Operation(summary = "Get admin dashboard stats")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(authService.getAdminStats());
    }

    @Operation(summary = "Update user status")
    @PutMapping("/users/{id}/status")
    public ResponseEntity<Void> setUserStatus(@PathVariable("id") Integer id, @RequestParam boolean active) {
        authService.setUserStatus(id, active);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update user plan")
    @PutMapping("/users/{id}/plan")
    public ResponseEntity<Void> updateUserPlan(@PathVariable("id") Integer id, @RequestParam String plan) {
        authService.updateSubscription(id, plan);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete user")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Integer id) {
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
