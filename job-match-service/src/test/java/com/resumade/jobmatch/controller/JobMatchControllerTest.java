package com.resumade.jobmatch.controller;

import com.resumade.jobmatch.entity.JobMatch;
import com.resumade.jobmatch.service.JobMatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobMatchControllerTest {

    @Mock
    private JobMatchService jobMatchService;

    @InjectMocks
    private JobMatchController jobMatchController;

    private JobMatch mockJobMatch;

    @BeforeEach
    void setUp() {
        mockJobMatch = new JobMatch();
        mockJobMatch.setMatchId(1L);
        mockJobMatch.setJobTitle("Java Dev");
    }

    @Test
    void searchJobs_shouldReturnList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(jobMatchService.searchJobs(1, "Java", "NY", "in", 1)).thenReturn(Arrays.asList(mockJobMatch));

        ResponseEntity<List<JobMatch>> response = jobMatchController.searchJobs("Java", "NY", "in", 1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void fetchLinkedIn_shouldReturnList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        
        Map<String, String> payload = new HashMap<>();
        payload.put("title", "Java");
        payload.put("location", "NY");
        
        when(jobMatchService.fetchJobsFromLinkedIn(1, "Java", "NY")).thenReturn(Arrays.asList(mockJobMatch));

        ResponseEntity<List<JobMatch>> response = jobMatchController.fetchLinkedIn(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void fetchNaukri_shouldReturnList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        
        Map<String, String> payload = new HashMap<>();
        payload.put("title", "Java");
        payload.put("location", "NY");
        
        when(jobMatchService.fetchJobsFromNaukri(1, "Java", "NY")).thenReturn(Arrays.asList(mockJobMatch));

        ResponseEntity<List<JobMatch>> response = jobMatchController.fetchNaukri(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void analyze_shouldReturnAnalyzedJob() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("resumeId", 1);
        payload.put("matchId", 1L);
        
        when(jobMatchService.analyzeJobFit(1, 1, 1L, "token")).thenReturn(mockJobMatch);

        ResponseEntity<JobMatch> response = jobMatchController.analyze(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void bookmark_shouldReturnOk() {
        ResponseEntity<Void> response = jobMatchController.bookmark(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(jobMatchService).toggleBookmark(1L);
    }

    @Test
    void getHistory_shouldReturnList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        
        when(jobMatchService.getUserHistory(1)).thenReturn(Arrays.asList(mockJobMatch));

        ResponseEntity<List<JobMatch>> response = jobMatchController.getHistory(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getBookmarks_shouldReturnList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        
        when(jobMatchService.getBookmarks(1)).thenReturn(Arrays.asList(mockJobMatch));

        ResponseEntity<List<JobMatch>> response = jobMatchController.getBookmarks(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
