package com.resumade.jobmatch.controller;

import com.resumade.jobmatch.entity.JobMatch;
import com.resumade.jobmatch.service.JobMatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/job-matches")
public class JobMatchController {

    private final JobMatchService jobMatchService;

    public JobMatchController(JobMatchService jobMatchService) {
        this.jobMatchService = jobMatchService;
    }

    /**
     * Public job search — available to all authenticated users (FREE + PREMIUM).
     * Accepts query params: title, location, country (default: "in" for India)
     */
    @GetMapping("/search")
    public ResponseEntity<List<JobMatch>> searchJobs(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "in") String country,
            @RequestParam(defaultValue = "1") Integer page,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        return ResponseEntity.ok(
                jobMatchService.searchJobs(userId, title, location, country, page));
    }

    @PostMapping("/fetch/linkedin")
    public ResponseEntity<List<JobMatch>> fetchLinkedIn(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        return ResponseEntity
                .ok(jobMatchService.fetchJobsFromLinkedIn(userId, payload.get("title"), payload.get("location")));
    }

    @PostMapping("/fetch/naukri")
    public ResponseEntity<List<JobMatch>> fetchNaukri(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        return ResponseEntity
                .ok(jobMatchService.fetchJobsFromNaukri(userId, payload.get("title"), payload.get("location")));
    }

    @PostMapping("/analyze")
    public ResponseEntity<JobMatch> analyze(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        String authHeader = request.getHeader("Authorization");
        String token = authHeader != null && authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : null;

        Object resumeIdObj = payload.get("resumeId");
        Integer resumeId = (resumeIdObj instanceof Number) ? ((Number) resumeIdObj).intValue() : null;

        return ResponseEntity.ok(jobMatchService.analyzeJobFit(
                userId,
                resumeId,
                Long.valueOf(payload.get("matchId").toString()),
                token));
    }

    @PostMapping("/{id}/bookmark")
    public ResponseEntity<Void> bookmark(@PathVariable Long id) {
        jobMatchService.toggleBookmark(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<JobMatch>> getHistory(HttpServletRequest request) {
        Integer userId = extractUserId(request);
        return ResponseEntity.ok(jobMatchService.getUserHistory(userId));
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<List<JobMatch>> getBookmarks(HttpServletRequest request) {
        Integer userId = extractUserId(request);
        return ResponseEntity.ok(jobMatchService.getBookmarks(userId));
    }

    @GetMapping("/test-jooble")
    public ResponseEntity<Map<String, Object>> testJooble(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(jobMatchService.testJooble(title, location));
    }

    private Integer extractUserId(HttpServletRequest request) {
        Object obj = request.getAttribute("userId");
        return (obj instanceof Number) ? ((Number) obj).intValue() : null;
    }
}
