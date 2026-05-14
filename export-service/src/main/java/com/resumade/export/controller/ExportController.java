package com.resumade.export.controller;

import com.resumade.export.entity.ExportJob;
import com.resumade.export.service.ExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exports")
// Export API — queues async PDF/DOCX/JSON generation jobs and serves file downloads
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping
    public ResponseEntity<ExportJob> requestExport(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        Integer userId = (userIdObj instanceof Number) ? ((Number) userIdObj).intValue() : null;
        
        // Fallback to X-User-Id header when userId attribute is missing (gateway-only auth)
        if (userId == null && request.getHeader("X-User-Id") != null) {
            try {
                userId = Integer.parseInt(request.getHeader("X-User-Id"));
            } catch (NumberFormatException ignored) {}
        }
        
        Object resumeIdObj = payload.get("resumeId");
        Integer resumeId = (resumeIdObj instanceof Number) ? ((Number) resumeIdObj).intValue() : null;
        
        String formatStr = (String) payload.get("format");
        ExportJob.ExportFormat format = ExportJob.ExportFormat.valueOf(formatStr.toUpperCase());

        ExportJob job = exportService.createExportJob(userId, resumeId, format);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<ExportJob> getStatus(@PathVariable UUID jobId) {
        return ResponseEntity.ok(exportService.getJobStatus(jobId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ExportJob>> getHistory(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        Integer userId = (userIdObj instanceof Number) ? ((Number) userIdObj).intValue() : null;
        
        // Fallback to header
        if (userId == null && request.getHeader("X-User-Id") != null) {
            try {
                userId = Integer.parseInt(request.getHeader("X-User-Id"));
            } catch (NumberFormatException ignored) {}
        }
        return ResponseEntity.ok(exportService.getUserHistory(userId));
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @PathVariable String filename) {
        // Path traversal guard — rejects filenames containing ".." or "/" to prevent directory escape
        if (filename.contains("..") || filename.contains("/")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Path baseDir = Paths.get("exports").toAbsolutePath().normalize();
            Path file = baseDir.resolve(filename).normalize();
            if (!file.startsWith(baseDir)) {
                return ResponseEntity.badRequest().build();
            }
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = filename.endsWith(".pdf") ? "application/pdf"
                    : filename.endsWith(".docx") ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    : filename.endsWith(".json") ? "application/json"
                    : "application/octet-stream";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
