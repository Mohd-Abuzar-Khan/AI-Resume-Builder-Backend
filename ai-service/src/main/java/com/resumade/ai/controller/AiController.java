package com.resumade.ai.controller;

import com.resumade.ai.dto.AtsReport;
import com.resumade.ai.service.AiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/summary")
    public ResponseEntity<String> generateSummary(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        String result = aiService.generateSummary(
                userId,
                extractInt(payload, "resumeId"),
                (String) payload.get("jobTitle"),
                extractInt(payload, "yearsExp"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bullets")
    public ResponseEntity<String> generateBullets(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        String result = aiService.generateBulletPoints(
                userId,
                extractInt(payload, "resumeId"),
                (String) payload.get("jobRole"),
                (String) payload.get("company"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ats-check")
    public ResponseEntity<AtsReport> checkAts(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        AtsReport report = aiService.checkAtsCompatibility(
                userId,
                extractInt(payload, "resumeId"),
                (String) payload.getOrDefault("resumeContent", ""),
                (String) payload.get("jobDescription"));
        return ResponseEntity.ok(report);
    }

    @GetMapping("/suggest-skills")
    public ResponseEntity<List<String>> suggestSkills(@RequestParam String jobTitle) {
        return ResponseEntity.ok(aiService.suggestSkills(jobTitle));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAi(@RequestParam String prompt, @RequestParam String type, HttpServletRequest request) {
        Integer userId = extractUserId(request);
        return aiService.streamAiResponse(userId, prompt, type);
    }

    @PostMapping("/cover-letter")
    public ResponseEntity<String> generateCoverLetter(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        String result = aiService.generateCoverLetter(
                userId,
                extractInt(payload, "resumeId"),
                (String) payload.get("jobDescription"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/tailor")
    public ResponseEntity<String> tailorResume(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        String result = aiService.tailorResumeForJob(
                userId,
                extractInt(payload, "resumeId"),
                (String) payload.getOrDefault("resumeContent", ""),
                (String) payload.get("jobDescription"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test")
    public ResponseEntity<String> testAi(@RequestBody Map<String, String> payload) {
        String prompt = payload.get("prompt");
        String result = aiService.testPrompt(prompt);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/improve-section")
    public ResponseEntity<String> improveSection(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        Integer userId = extractUserId(request);
        String result = aiService.improveSection(
                userId,
                (String) payload.get("content"),
                (String) payload.getOrDefault("tone", "professional"));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<com.resumade.ai.entity.AiRequest>> getHistory(HttpServletRequest request) {
        Integer userId = extractUserId(request);
        return ResponseEntity.ok(aiService.getUserHistory(userId));
    }

    // Safe extraction helpers to handle Integer/Long from JSON
    private Integer extractUserId(HttpServletRequest request) {
        Object obj = request.getAttribute("userId");
        return (obj instanceof Number) ? ((Number) obj).intValue() : null;
    }

    private Integer extractInt(Map<String, Object> payload, String key) {
        Object obj = payload.get(key);
        return (obj instanceof Number) ? ((Number) obj).intValue() : null;
    }
}
