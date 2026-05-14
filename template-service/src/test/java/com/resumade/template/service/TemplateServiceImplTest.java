package com.resumade.template.service;

import com.resumade.template.dto.TemplateRequest;
import com.resumade.template.dto.TemplateResponse;
import com.resumade.template.entity.Template;
import com.resumade.template.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceImplTest {

    @Mock
    private TemplateRepository templateRepository;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private Template testTemplate;

    @BeforeEach
    void setUp() {
        testTemplate = new Template("Test", "Desc", "url", "html", "css", Template.Category.PROFESSIONAL, false);
        testTemplate.setTemplateId(1);
    }

    @Test
    void seedDefaultTemplates_shouldSeedWhenEmpty() {
        when(templateRepository.count()).thenReturn(0L);
        templateService.seedDefaultTemplates();
        verify(templateRepository).saveAll(any());
    }

    @Test
    void seedDefaultTemplates_shouldNotSeedWhenNotEmpty() {
        when(templateRepository.count()).thenReturn(3L);
        templateService.seedDefaultTemplates();
        verify(templateRepository, never()).saveAll(any());
    }

    @Test
    void getAllActiveTemplates_shouldReturnList() {
        when(templateRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(testTemplate));
        List<TemplateResponse> result = templateService.getAllActiveTemplates();
        assertEquals(1, result.size());
    }

    @Test
    void getFreeTemplates_shouldReturnList() {
        when(templateRepository.findByIsActiveTrueAndIsPremiumFalse()).thenReturn(Arrays.asList(testTemplate));
        List<TemplateResponse> result = templateService.getFreeTemplates();
        assertEquals(1, result.size());
    }

    @Test
    void getPremiumTemplates_shouldReturnList() {
        when(templateRepository.findByIsActiveTrueAndIsPremiumTrue()).thenReturn(Arrays.asList(testTemplate));
        List<TemplateResponse> result = templateService.getPremiumTemplates();
        assertEquals(1, result.size());
    }

    @Test
    void getTemplatesByCategory_shouldReturnList() {
        when(templateRepository.findByIsActiveTrueAndCategory(Template.Category.PROFESSIONAL)).thenReturn(Arrays.asList(testTemplate));
        List<TemplateResponse> result = templateService.getTemplatesByCategory("PROFESSIONAL");
        assertEquals(1, result.size());
    }

    @Test
    void getPopularTemplates_shouldReturnList() {
        when(templateRepository.findByIsActiveTrueOrderByUsageCountDesc()).thenReturn(Arrays.asList(testTemplate));
        List<TemplateResponse> result = templateService.getPopularTemplates();
        assertEquals(1, result.size());
    }

    @Test
    void getTemplateById_shouldReturnTemplate() {
        when(templateRepository.findById(1)).thenReturn(Optional.of(testTemplate));
        TemplateResponse result = templateService.getTemplateById(1);
        assertEquals("Test", result.getName());
    }

    @Test
    void getTemplateById_shouldThrowWhenNotFound() {
        when(templateRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> templateService.getTemplateById(1));
    }

    @Test
    void createTemplate_shouldSaveTemplate() {
        TemplateRequest request = new TemplateRequest();
        request.setName("New Template");
        request.setHtmlLayout("");
        request.setCssStyles("");
        when(templateRepository.save(any())).thenReturn(testTemplate);

        TemplateResponse result = templateService.createTemplate(request, "ADMIN");
        assertNotNull(result);
    }

    @Test
    void createTemplate_shouldThrowWhenNotAdmin() {
        TemplateRequest request = new TemplateRequest();
        assertThrows(RuntimeException.class, () -> templateService.createTemplate(request, "USER"));
    }

    @Test
    void updateTemplate_shouldUpdateAndSave() {
        when(templateRepository.findById(1)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(any())).thenReturn(testTemplate);

        TemplateRequest request = new TemplateRequest();
        request.setName("Updated");
        request.setHtmlLayout("new html");
        request.setCssStyles("new css");
        request.setCategory(Template.Category.CREATIVE);
        request.setIsPremium(true);
        request.setHasPhoto(true);
        request.setHasSkillBars(true);
        request.setIsActive(true);
        request.setLayoutConfig("{}");

        TemplateResponse result = templateService.updateTemplate(1, request, "ADMIN");
        assertNotNull(result);
    }

    @Test
    void updateTemplate_shouldThrowWhenNotAdmin() {
        TemplateRequest request = new TemplateRequest();
        assertThrows(RuntimeException.class, () -> templateService.updateTemplate(1, request, "USER"));
    }

    @Test
    void deactivateTemplate_shouldSetInactive() {
        when(templateRepository.findById(1)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(any())).thenReturn(testTemplate);

        templateService.deactivateTemplate(1, "ADMIN");
        assertFalse(testTemplate.getIsActive());
    }

    @Test
    void incrementUsage_shouldIncrement() {
        testTemplate.setUsageCount(0);
        when(templateRepository.findById(1)).thenReturn(Optional.of(testTemplate));
        when(templateRepository.save(any())).thenReturn(testTemplate);

        templateService.incrementUsage(1);
        assertEquals(1, testTemplate.getUsageCount());
    }
}
