package com.resumade.resume.controller;

import com.resumade.resume.service.ResumeService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResumeControllerTest {

    @Mock
    private ResumeService resumeService;

    @InjectMocks
    private AdminResumeController adminResumeController;

    @Test
    void getAdminStats_shouldReturnStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalResumes", 100L);
        when(resumeService.getAdminStats()).thenReturn(stats);

        ResponseEntity<Map<String, Object>> response = adminResumeController.getAdminStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(100L, response.getBody().get("totalResumes"));
        verify(resumeService).getAdminStats();
    }
}
