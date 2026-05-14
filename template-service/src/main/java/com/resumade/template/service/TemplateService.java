package com.resumade.template.service;

import com.resumade.template.dto.TemplateRequest;
import com.resumade.template.dto.TemplateResponse;

import java.util.List;

public interface TemplateService {
    List<TemplateResponse> getAllActiveTemplates();

    List<TemplateResponse> getFreeTemplates();

    List<TemplateResponse> getPremiumTemplates();

    List<TemplateResponse> getTemplatesByCategory(String category);

    List<TemplateResponse> getPopularTemplates();

    TemplateResponse getTemplateById(Integer id);

    TemplateResponse createTemplate(TemplateRequest request, String role);

    TemplateResponse updateTemplate(Integer id, TemplateRequest request, String role);

    void deactivateTemplate(Integer id, String role);

    void incrementUsage(Integer id);
}
