package com.resumade.template.service;

import com.resumade.template.dto.TemplateRequest;
import com.resumade.template.entity.Template;

import java.util.List;

public interface TemplateService {
    List<Template> getAllActiveTemplates();
    List<Template> getFreeTemplates();
    List<Template> getPremiumTemplates();
    List<Template> getTemplatesByCategory(String category);
    List<Template> getPopularTemplates();
    Template getTemplateById(Integer id);
    
    Template createTemplate(TemplateRequest request, String role);
    Template updateTemplate(Integer id, TemplateRequest request, String role);
    void deactivateTemplate(Integer id, String role);
    void incrementUsage(Integer id);
}
