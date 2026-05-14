package com.resumade.resume.controller;

import com.resumade.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/resumes/admin")
@Tag(name = "Admin Resumes", description = "Admin resume management endpoints")
@RequiredArgsConstructor
public class AdminResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "Get admin resume stats")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getAdminStats() {
        return ResponseEntity.ok(resumeService.getAdminStats());
    }
}
