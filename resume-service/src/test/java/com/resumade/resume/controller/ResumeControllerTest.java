package com.resumade.resume.controller;

import com.resumade.resume.dto.ResumeRequest;
import com.resumade.resume.dto.ResumeResponse;
import com.resumade.resume.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    @Mock
    private ResumeService resumeService;

    @InjectMocks
    private ResumeController resumeController;

    private ResumeResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = ResumeResponse.builder()
                .resumeId(1)
                .title("My Resume")
                .targetJobTitle("Software Engineer")
                .templateId(1)
                .build();
    }

    @Test
    void createResume_shouldReturnCreated() {
        ResumeRequest request = new ResumeRequest();
        request.setTitle("New Resume");
        request.setTargetJobTitle("Developer");

        when(resumeService.createResume(eq(1), eq("FREE"), any(ResumeRequest.class))).thenReturn(mockResponse);

        ResponseEntity<ResumeResponse> response = resumeController.createResume(1, "FREE", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getUserResumes_shouldReturnList() {
        when(resumeService.getUserResumes(1)).thenReturn(Arrays.asList(mockResponse));

        ResponseEntity<List<ResumeResponse>> response = resumeController.getUserResumes(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getResumeById_shouldReturnResume() {
        when(resumeService.getResumeById(1, 1)).thenReturn(mockResponse);

        ResponseEntity<ResumeResponse> response = resumeController.getResumeById(1, 1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateResume_shouldReturnUpdated() {
        ResumeRequest request = new ResumeRequest();
        request.setTitle("Updated");
        when(resumeService.updateResume(eq(1), eq(1), any())).thenReturn(mockResponse);

        ResponseEntity<ResumeResponse> response = resumeController.updateResume(1, 1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteResume_shouldReturnNoContent() {
        ResponseEntity<Void> response = resumeController.deleteResume(1, 1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(resumeService).deleteResume(1, 1);
    }

    @Test
    void duplicateResume_shouldReturnCreated() {
        when(resumeService.duplicateResume(1, 1, "FREE")).thenReturn(mockResponse);

        ResponseEntity<ResumeResponse> response = resumeController.duplicateResume(1, "FREE", 1);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void publishResume_shouldReturnOk() {
        when(resumeService.publishResume(1, 1, true, "John", null)).thenReturn(mockResponse);

        ResponseEntity<ResumeResponse> response = resumeController.publishResume(1, 1, true, "John", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPublicResumes_shouldReturnListWithQuery() {
        when(resumeService.getPublicResumes("engineer")).thenReturn(Arrays.asList(mockResponse));

        ResponseEntity<List<ResumeResponse>> response = resumeController.getPublicResumes("engineer");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getPublicResumes_shouldReturnListWithoutQuery() {
        when(resumeService.getPublicResumes(null)).thenReturn(Arrays.asList(mockResponse));

        ResponseEntity<List<ResumeResponse>> response = resumeController.getPublicResumes(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
