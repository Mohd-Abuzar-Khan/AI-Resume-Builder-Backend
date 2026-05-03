package com.resumade.template.controller;

import com.resumade.template.dto.TemplateRequest;
import com.resumade.template.entity.Template;
import com.resumade.template.service.TemplateService;
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
class TemplateControllerTest {

    @Mock
    private TemplateService templateService;

    @InjectMocks
    private TemplateController templateController;

    private Template mockTemplate;

    @BeforeEach
    void setUp() {
        mockTemplate = new Template("Test Template", "Desc", "url", "html", "css", Template.Category.PROFESSIONAL, false);
    }

    @Test
    void getAllTemplates_shouldReturnList() {
        when(templateService.getAllActiveTemplates()).thenReturn(Arrays.asList(mockTemplate));
        ResponseEntity<List<Template>> response = templateController.getAllTemplates();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getFreeTemplates_shouldReturnList() {
        when(templateService.getFreeTemplates()).thenReturn(Arrays.asList(mockTemplate));
        ResponseEntity<List<Template>> response = templateController.getFreeTemplates();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPremiumTemplates_shouldReturnList() {
        when(templateService.getPremiumTemplates()).thenReturn(Arrays.asList(mockTemplate));
        ResponseEntity<List<Template>> response = templateController.getPremiumTemplates();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTemplatesByCategory_shouldReturnList() {
        when(templateService.getTemplatesByCategory("PROFESSIONAL")).thenReturn(Arrays.asList(mockTemplate));
        ResponseEntity<List<Template>> response = templateController.getTemplatesByCategory("PROFESSIONAL");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPopularTemplates_shouldReturnList() {
        when(templateService.getPopularTemplates()).thenReturn(Arrays.asList(mockTemplate));
        ResponseEntity<List<Template>> response = templateController.getPopularTemplates();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getTemplateById_shouldReturnTemplate() {
        when(templateService.getTemplateById(1)).thenReturn(mockTemplate);
        ResponseEntity<Template> response = templateController.getTemplateById(1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void incrementUsage_shouldReturnOk() {
        ResponseEntity<Void> response = templateController.incrementUsage(1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(templateService).incrementUsage(1);
    }

    @Test
    void createTemplate_shouldReturnCreatedWhenAdmin() {
        TemplateRequest request = new TemplateRequest();
        when(templateService.createTemplate(any(), eq("ADMIN"))).thenReturn(mockTemplate);
        ResponseEntity<Template> response = templateController.createTemplate("ADMIN", request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void createTemplate_shouldReturnForbiddenWhenNotAdmin() {
        TemplateRequest request = new TemplateRequest();
        ResponseEntity<Template> response = templateController.createTemplate("USER", request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void updateTemplate_shouldReturnOkWhenAdmin() {
        TemplateRequest request = new TemplateRequest();
        when(templateService.updateTemplate(eq(1), any(), eq("ADMIN"))).thenReturn(mockTemplate);
        ResponseEntity<Template> response = templateController.updateTemplate(1, "ADMIN", request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateTemplate_shouldReturnForbiddenWhenNotAdmin() {
        TemplateRequest request = new TemplateRequest();
        ResponseEntity<Template> response = templateController.updateTemplate(1, "USER", request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void deactivateTemplate_shouldReturnOkWhenAdmin() {
        ResponseEntity<Void> response = templateController.deactivateTemplate(1, "ADMIN");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(templateService).deactivateTemplate(1, "ADMIN");
    }

    @Test
    void deactivateTemplate_shouldReturnForbiddenWhenNotAdmin() {
        ResponseEntity<Void> response = templateController.deactivateTemplate(1, "USER");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
