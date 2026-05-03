package com.resumade.resume.controller;

import com.resumade.resume.dto.SectionOrderRequest;
import com.resumade.resume.dto.SectionRequest;
import com.resumade.resume.entity.ResumeSection;
import com.resumade.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sections")
@Tag(name = "Resume Sections", description = "Endpoints for managing sections within a resume")
public class SectionController {

    private final ResumeService resumeService;

    public SectionController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @Operation(summary = "Add a new section to a resume")
    @PostMapping("/resume/{resumeId}")
    public ResponseEntity<ResumeSection> addSection(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("resumeId") Integer resumeId,
            @Valid @RequestBody SectionRequest request) {
        ResumeSection created = resumeService.addSection(resumeId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update a section")
    @PutMapping("/{id}")
    public ResponseEntity<ResumeSection> updateSection(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer id,
            @Valid @RequestBody SectionRequest request) {
        return ResponseEntity.ok(resumeService.updateSection(id, userId, request));
    }

    @Operation(summary = "Delete a section")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSection(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer id) {
        resumeService.deleteSection(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reorder sections")
    @PutMapping("/resume/{resumeId}/reorder")
    public ResponseEntity<Void> reorderSections(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("resumeId") Integer resumeId,
            @Valid @RequestBody List<SectionOrderRequest> requests) {
        resumeService.reorderSections(resumeId, userId, requests);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Toggle section visibility")
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ResumeSection> toggleVisibility(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer id,
            @RequestParam("isVisible") boolean isVisible) {
        return ResponseEntity.ok(resumeService.toggleSectionVisibility(id, userId, isVisible));
    }
}
