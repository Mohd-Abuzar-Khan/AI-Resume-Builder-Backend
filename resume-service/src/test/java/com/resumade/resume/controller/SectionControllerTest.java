package com.resumade.resume.controller;

import com.resumade.resume.dto.SectionOrderRequest;
import com.resumade.resume.dto.SectionRequest;
import com.resumade.resume.dto.SectionResponse;
import com.resumade.resume.entity.ResumeSection;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionControllerTest {

    @Mock
    private ResumeService resumeService;

    @InjectMocks
    private SectionController sectionController;

    private SectionResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = SectionResponse.builder()
                .sectionId(1)
                .sectionType(ResumeSection.SectionType.EXPERIENCE)
                .title("Work Experience")
                .content("content")
                .displayOrder(1)
                .build();
    }

    @Test
    void addSection_shouldReturnCreated() {
        SectionRequest request = new SectionRequest();
        when(resumeService.addSection(eq(1), eq(1), any(SectionRequest.class))).thenReturn(mockResponse);

        ResponseEntity<SectionResponse> response = sectionController.addSection(1, 1, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1, response.getBody().getSectionId());
    }

    @Test
    void updateSection_shouldReturnOk() {
        SectionRequest request = new SectionRequest();
        when(resumeService.updateSection(eq(1), eq(1), any(SectionRequest.class))).thenReturn(mockResponse);

        ResponseEntity<SectionResponse> response = sectionController.updateSection(1, 1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteSection_shouldReturnNoContent() {
        ResponseEntity<Void> response = sectionController.deleteSection(1, 1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(resumeService).deleteSection(1, 1);
    }

    @Test
    void reorderSections_shouldReturnOk() {
        List<SectionOrderRequest> requests = Arrays.asList(new SectionOrderRequest());
        ResponseEntity<Void> response = sectionController.reorderSections(1, 1, requests);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(resumeService).reorderSections(1, 1, requests);
    }

    @Test
    void toggleVisibility_shouldReturnOk() {
        when(resumeService.toggleSectionVisibility(1, 1, true)).thenReturn(mockResponse);

        ResponseEntity<SectionResponse> response = sectionController.toggleVisibility(1, 1, true);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(resumeService).toggleSectionVisibility(1, 1, true);
    }
}
