package com.resumade.ai.controller;

import com.resumade.ai.dto.AtsReport;
import com.resumade.ai.service.AiService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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
        when(aiService.generateCoverLetter(1, 1, "JD")).thenReturn("Letter");

        Map<String, Object> payload = new HashMap<>();
        payload.put("resumeId", 1);
        payload.put("jobDescription", "JD");

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
}
