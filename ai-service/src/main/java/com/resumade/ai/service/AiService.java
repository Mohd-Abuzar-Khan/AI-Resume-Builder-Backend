package com.resumade.ai.service;

import com.resumade.ai.dto.AtsReport;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiService {
    String generateSummary(Integer userId, Integer resumeId, String jobTitle, int yearsExp);

    String generateBulletPoints(Integer userId, Integer resumeId, String jobRole, String company);

    AtsReport checkAtsCompatibility(Integer userId, Integer resumeId, String resumeContent, String jobDescription);

    List<String> suggestSkills(String jobTitle);

    String generateCoverLetter(Integer userId, Integer resumeId, String jobDescription);

    String improveSection(Integer userId, String sectionContent, String tone);

    String tailorResumeForJob(Integer userId, Integer resumeId, String resumeContent, String jobDescription);

    String translateResume(Integer userId, Integer resumeId, String targetLanguage);

    // SSE Streaming
    Flux<String> streamAiResponse(Integer userId, String prompt, String requestType);

    String testPrompt(String prompt);

    List<com.resumade.ai.entity.AiRequest> getUserHistory(Integer userId);
}
