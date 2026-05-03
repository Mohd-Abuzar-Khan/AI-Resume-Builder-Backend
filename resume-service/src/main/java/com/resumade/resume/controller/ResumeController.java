package com.resumade.resume.controller;

import com.resumade.resume.dto.ResumeRequest;
import com.resumade.resume.entity.Resume;
import com.resumade.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@Tag(name = "Resumes", description = "Resume management endpoints")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @Operation(summary = "Create a new resume")
    @PostMapping
    public ResponseEntity<Resume> createResume(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @Valid @RequestBody ResumeRequest request) {
        Resume created = resumeService.createResume(userId, plan, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get user's resumes")
    @GetMapping
    public ResponseEntity<List<Resume>> getUserResumes(@RequestHeader("X-User-Id") Integer userId) {
        return ResponseEntity.ok(resumeService.getUserResumes(userId));
    }

    @Operation(summary = "Get a specific resume")
    @GetMapping("/{id}")
    public ResponseEntity<Resume> getResumeById(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @PathVariable("id") Integer id) {
        return ResponseEntity.ok(resumeService.getResumeById(id, userId));
    }

    @Operation(summary = "Update a resume")
    @PutMapping("/{id}")
    public ResponseEntity<Resume> updateResume(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer id,
            @Valid @RequestBody ResumeRequest request) {
        return ResponseEntity.ok(resumeService.updateResume(id, userId, request));
    }

    @Operation(summary = "Delete a resume")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResume(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer id) {
        resumeService.deleteResume(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Duplicate a resume")
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<Resume> duplicateResume(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestHeader(value = "X-User-Plan", defaultValue = "FREE") String plan,
            @PathVariable("id") Integer id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeService.duplicateResume(id, userId, plan));
    }

    @Operation(summary = "Publish a resume")
    @PutMapping("/{id}/publish")
    public ResponseEntity<Resume> publishResume(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable("id") Integer id,
            @RequestParam("isPublic") boolean isPublic,
            @RequestParam(value = "ownerName", required = false) String ownerName,
            @RequestParam(value = "ownerAvatar", required = false) String ownerAvatar) {
        return ResponseEntity.ok(resumeService.publishResume(id, userId, isPublic, ownerName, ownerAvatar));
    }

    @Operation(summary = "Get public gallery resumes with optional search")
    @GetMapping("/public")
    public ResponseEntity<List<Resume>> getPublicResumes(@RequestParam(value = "q", required = false) String q) {
        return ResponseEntity.ok(resumeService.getPublicResumes(q));
    }
}
