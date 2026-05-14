package com.resumade.ai.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.resumade.ai.dto.AtsReport;
import com.resumade.ai.service.AiService;

import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiService aiService;

    @InjectMocks
    private AiController aiController;

    @Test
    void checkAts_shouldReturnReport() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("resumeId", 1);
        payload.put("resumeContent", "content");
        payload.put("jobDescription", "JD");

        AtsReport mockReport = new AtsReport();
        when(aiService.checkAtsCompatibility(1, 1, "content", "JD")).thenReturn(mockReport);

        ResponseEntity<AtsReport> response = aiController.checkAts(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void generateSummary_shouldReturnString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(aiService.generateSummary(1, 1, "Dev", 5)).thenReturn("Summary");

        Map<String, Object> payload = new HashMap<>();
        payload.put("resumeId", 1);
        payload.put("jobTitle", "Dev");
        payload.put("yearsExp", 5);

        ResponseEntity<String> response = aiController.generateSummary(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Summary", response.getBody());
    }

    @Test
    void generateBullets_shouldReturnString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(aiService.generateBulletPoints(1, 1, "Dev", "Comp")).thenReturn("Bullets");

        Map<String, Object> payload = new HashMap<>();
        payload.put("resumeId", 1);
        payload.put("jobRole", "Dev");
        payload.put("company", "Comp");

        ResponseEntity<String> response = aiController.generateBullets(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Bullets", response.getBody());
    }

    @Test
    void improveSection_shouldReturnString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(aiService.improveSection(1, "Text", "professional")).thenReturn("Improved");

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "Text");
        payload.put("tone", "professional");

        ResponseEntity<String> response = aiController.improveSection(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Improved", response.getBody());
    }

    @Test
    void generateCoverLetter_shouldReturnString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(aiService.generateCoverLetter(1, 1, "resumeContent", "JD")).thenReturn("Letter");

        Map<String, Object> payload = new HashMap<>();
        payload.put("resumeId", 1);
        payload.put("jobDescription", "JD");
        payload.put("resumeContent", "resumeContent");

        ResponseEntity<String> response = aiController.generateCoverLetter(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Letter", response.getBody());
    }

    @Test
    void tailorResume_shouldReturnString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(aiService.tailorResumeForJob(1, 1, "Content", "JD")).thenReturn("Tailored");

        Map<String, Object> payload = new HashMap<>();
        payload.put("resumeId", 1);
        payload.put("resumeContent", "Content");
        payload.put("jobDescription", "JD");

        ResponseEntity<String> response = aiController.tailorResume(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Tailored", response.getBody());
    }

    @Test
    void suggestSkills_shouldReturnList() {
        when(aiService.suggestSkills("Developer")).thenReturn(List.of("Java", "Spring"));

        ResponseEntity<List<String>> response = aiController.suggestSkills("Developer");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void streamAi_shouldReturnFlux() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(aiService.streamAiResponse(1, "Prompt", "SUMMARY")).thenReturn(Flux.just("chunk"));

        List<String> result = aiController.streamAi("Prompt", "SUMMARY", request)
                .collectList()
                .block();

        assertEquals(1, result.size());
        assertEquals("chunk", result.get(0));
    }

    @Test
    void testAi_shouldReturnString() {
        when(aiService.testPrompt("Hello")).thenReturn("OK");

        Map<String, String> payload = new HashMap<>();
        payload.put("prompt", "Hello");

        ResponseEntity<String> response = aiController.testAi(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
    }

    @Test
    void getHistory_shouldReturnList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(aiService.getUserHistory(1)).thenReturn(List.of(new com.resumade.ai.entity.AiRequest()));

        ResponseEntity<List<com.resumade.ai.entity.AiRequest>> response = aiController.getHistory(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}
