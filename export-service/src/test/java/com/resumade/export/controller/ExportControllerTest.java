package com.resumade.export.controller;

import com.resumade.export.entity.ExportJob;
import com.resumade.export.service.ExportService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController exportController;

    private ExportJob mockJob;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        jobId = UUID.randomUUID();
        mockJob = new ExportJob(1, 1, ExportJob.ExportFormat.PDF, ExportJob.ExportStatus.COMPLETED);
        mockJob.setJobId(jobId);
    }

    @Test
    void requestExport_shouldReturnJob() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("resumeId", 1);
        payload.put("format", "PDF");

        when(exportService.createExportJob(1, 1, ExportJob.ExportFormat.PDF)).thenReturn(mockJob);

        ResponseEntity<ExportJob> response = exportController.requestExport(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(jobId, response.getBody().getJobId());
    }

    @Test
    void requestExport_shouldFallbackToHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(null);
        when(request.getHeader("X-User-Id")).thenReturn("1");
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("resumeId", 1);
        payload.put("format", "PDF");

        when(exportService.createExportJob(1, 1, ExportJob.ExportFormat.PDF)).thenReturn(mockJob);

        ResponseEntity<ExportJob> response = exportController.requestExport(payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getStatus_shouldReturnJob() {
        when(exportService.getJobStatus(jobId)).thenReturn(mockJob);

        ResponseEntity<ExportJob> response = exportController.getStatus(jobId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(jobId, response.getBody().getJobId());
    }

    @Test
    void getHistory_shouldReturnList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("userId")).thenReturn(1);
        
        when(exportService.getUserHistory(1)).thenReturn(Arrays.asList(mockJob));

        ResponseEntity<List<ExportJob>> response = exportController.getHistory(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void downloadFile_shouldReturnBadRequestForTraversal() {
        ResponseEntity<Resource> response = exportController.downloadFile("../test.pdf");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
