package com.resumade.template.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.resumade.template.dto.TemplateRequest;
import com.resumade.template.entity.Template;
import com.resumade.template.service.TemplateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "Templates", description = "Template management endpoints")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "Get all active templates")
    @GetMapping
    public ResponseEntity<List<Template>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllActiveTemplates());
    }

    @Operation(summary = "Get free templates")
    @GetMapping("/free")
    public ResponseEntity<List<Template>> getFreeTemplates() {
        return ResponseEntity.ok(templateService.getFreeTemplates());
    }

    @Operation(summary = "Get premium templates")
    @GetMapping("/premium")
    public ResponseEntity<List<Template>> getPremiumTemplates() {
        return ResponseEntity.ok(templateService.getPremiumTemplates());
    }

    @Operation(summary = "Get templates by category")
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Template>> getTemplatesByCategory(@PathVariable("category") String category) {
        return ResponseEntity.ok(templateService.getTemplatesByCategory(category));
    }

    @Operation(summary = "Get popular templates")
    @GetMapping("/popular")
    public ResponseEntity<List<Template>> getPopularTemplates() {
        return ResponseEntity.ok(templateService.getPopularTemplates());
    }

    @Operation(summary = "Get template by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Template> getTemplateById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }

    @Operation(summary = "Increment usage count for a template")
    @PostMapping("/{id}/usage")
    public ResponseEntity<Void> incrementUsage(@PathVariable("id") Integer id) {
        templateService.incrementUsage(id);
        return ResponseEntity.ok().build();
    }

    // Admin endpoints
    
    @Operation(summary = "Create a new template (Admin Only)")
    @PostMapping
    public ResponseEntity<Template> createTemplate(
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @Valid @RequestBody TemplateRequest request) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Template created = templateService.createTemplate(request, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update an existing template (Admin Only)")
    @PutMapping("/{id}")
    public ResponseEntity<Template> updateTemplate(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role,
            @Valid @RequestBody TemplateRequest request) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Template updated = templateService.updateTemplate(id, request, role);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Deactivate a template (Admin Only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateTemplate(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        if (!isAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        templateService.deactivateTemplate(id, role);
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
