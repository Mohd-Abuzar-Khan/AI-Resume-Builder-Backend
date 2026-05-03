package com.resumade.template.service;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.resumade.template.dto.TemplateRequest;
import com.resumade.template.entity.Template;
import com.resumade.template.repository.TemplateRepository;

@Service
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;

        private static final String DEFAULT_HTML_LAYOUT = "<div class=\"resume-root\">{{content}}</div>";
        private static final String DEFAULT_CSS_STYLES = ".resume-root { font-family: sans-serif; }";

        private static final String CLASSIC_PROFESSIONAL_CONFIG = """
                {
                    "font": {
                        "heading": "Playfair Display",
                        "body": "Source Sans Pro",
                        "mono": "JetBrains Mono",
                        "baseSize": 11,
                        "lineHeight": 1.5
                    },
                    "colors": {
                        "accent": "#1F3A6E",
                        "headingText": "#111111",
                        "bodyText": "#333333",
                        "mutedText": "#666666",
                        "divider": "#CCCCCC",
                        "background": "#FFFFFF",
                        "sidebarBackground": "#F4F6FA"
                    },
                    "page": {
                        "marginTop": 36,
                        "marginBottom": 36,
                        "marginLeft": 48,
                        "marginRight": 48,
                        "layout": "single-column"
                    },
                    "header": {
                        "nameSize": 28,
                        "nameBold": true,
                        "nameColor": "#111111",
                        "subtitleSize": 13,
                        "subtitleColor": "#555555",
                        "contactSize": 10,
                        "contactLayout": "inline",
                        "showDivider": true,
                        "dividerColor": "#1F3A6E",
                        "dividerWeight": 2
                    },
                    "sections": [
                        {
                            "type": "SUMMARY",
                            "label": "Professional Summary",
                            "enabled": true,
                            "order": 1,
                            "style": {
                                "labelSize": 13,
                                "labelBold": true,
                                "labelUppercase": true,
                                "labelColor": "#1F3A6E",
                                "showUnderline": true,
                                "underlineColor": "#1F3A6E",
                                "underlineWeight": 1.5,
                                "bodySize": 11,
                                "bodyColor": "#333333",
                                "spacingAfter": 14
                            }
                        },
                        {
                            "type": "EXPERIENCE",
                            "label": "Experience",
                            "enabled": true,
                            "order": 2,
                            "style": {
                                "labelSize": 13,
                                "labelBold": true,
                                "labelUppercase": true,
                                "labelColor": "#1F3A6E",
                                "showUnderline": true,
                                "underlineColor": "#1F3A6E",
                                "underlineWeight": 1.5,
                                "entryTitleSize": 12,
                                "entryTitleBold": true,
                                "entryTitleColor": "#111111",
                                "entrySubtitleSize": 11,
                                "entrySubtitleItalic": true,
                                "entrySubtitleColor": "#444444",
                                "entryDateSize": 10,
                                "entryDateColor": "#666666",
                                "bulletSize": 10,
                                "bulletColor": "#333333",
                                "bulletIndent": 14,
                                "spacingAfter": 14
                            }
                        },
                        {
                            "type": "EDUCATION",
                            "label": "Education",
                            "enabled": true,
                            "order": 3,
                            "style": {
                                "labelSize": 13,
                                "labelBold": true,
                                "labelUppercase": true,
                                "labelColor": "#1F3A6E",
                                "showUnderline": true,
                                "underlineColor": "#1F3A6E",
                                "underlineWeight": 1.5,
                                "entryTitleSize": 12,
                                "entryTitleBold": true,
                                "entrySubtitleItalic": true,
                                "spacingAfter": 14
                            }
                        },
                        {
                            "type": "SKILLS",
                            "label": "Skills",
                            "enabled": true,
                            "order": 4,
                            "style": {
                                "labelSize": 13,
                                "labelBold": true,
                                "labelUppercase": true,
                                "labelColor": "#1F3A6E",
                                "showUnderline": true,
                                "renderAs": "tags",
                                "tagBackground": "#EEF2FA",
                                "tagTextColor": "#1F3A6E",
                                "tagBorderRadius": 4,
                                "tagFontSize": 10,
                                "spacingAfter": 14
                            }
                        },
                        {
                            "type": "LANGUAGES",
                            "label": "Languages",
                            "enabled": true,
                            "order": 5,
                            "style": {
                                "labelSize": 13,
                                "labelBold": true,
                                "labelUppercase": true,
                                "renderAs": "inline",
                                "spacingAfter": 10
                            }
                        }
                    ],
                    "twoColumn": {
                        "enabled": false,
                        "splitRatio": 0.35,
                        "mainSections": ["SUMMARY", "EXPERIENCE", "EDUCATION"],
                        "sidebarSections": ["SKILLS", "LANGUAGES", "CERTIFICATIONS"],
                        "sidebarBackground": "#F4F6FA",
                        "sidebarPadding": 16
                    }
                }
                """;

        private static final String MODERN_MINIMAL_CONFIG = """
                {
                    "font": {
                        "heading": "Inter",
                        "body": "Inter",
                        "mono": "JetBrains Mono",
                        "baseSize": 11,
                        "lineHeight": 1.55
                    },
                    "colors": {
                        "accent": "#2C2C2C",
                        "headingText": "#111111",
                        "bodyText": "#333333",
                        "mutedText": "#666666",
                        "divider": "#DDDDDD",
                        "background": "#FFFFFF",
                        "sidebarBackground": "#F4F6FA"
                    },
                    "page": {
                        "marginTop": 34,
                        "marginBottom": 34,
                        "marginLeft": 46,
                        "marginRight": 46,
                        "layout": "single-column"
                    },
                    "header": {
                        "nameSize": 26,
                        "nameBold": true,
                        "nameColor": "#111111",
                        "subtitleSize": 12,
                        "subtitleColor": "#555555",
                        "contactSize": 10,
                        "contactLayout": "inline",
                        "showDivider": false,
                        "dividerColor": "#2C2C2C",
                        "dividerWeight": 1
                    },
                    "sections": [
                        {
                            "type": "SUMMARY",
                            "label": "Summary",
                            "enabled": true,
                            "order": 1,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": false,
                                "labelColor": "#2C2C2C",
                                "showUnderline": false,
                                "borderLeft": "3px solid #2C2C2C",
                                "bodySize": 11,
                                "bodyColor": "#333333",
                                "spacingAfter": 12
                            }
                        },
                        {
                            "type": "EXPERIENCE",
                            "label": "Experience",
                            "enabled": true,
                            "order": 2,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": false,
                                "labelColor": "#2C2C2C",
                                "showUnderline": false,
                                "borderLeft": "3px solid #2C2C2C",
                                "entryTitleSize": 12,
                                "entryTitleBold": true,
                                "entryTitleColor": "#111111",
                                "entrySubtitleSize": 11,
                                "entrySubtitleItalic": true,
                                "entrySubtitleColor": "#444444",
                                "entryDateSize": 10,
                                "entryDateColor": "#666666",
                                "bulletSize": 10,
                                "bulletColor": "#333333",
                                "bulletIndent": 14,
                                "spacingAfter": 12
                            }
                        },
                        {
                            "type": "EDUCATION",
                            "label": "Education",
                            "enabled": true,
                            "order": 3,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": false,
                                "labelColor": "#2C2C2C",
                                "showUnderline": false,
                                "borderLeft": "3px solid #2C2C2C",
                                "entryTitleSize": 12,
                                "entryTitleBold": true,
                                "entrySubtitleItalic": true,
                                "spacingAfter": 12
                            }
                        },
                        {
                            "type": "SKILLS",
                            "label": "Skills",
                            "enabled": true,
                            "order": 4,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": false,
                                "labelColor": "#2C2C2C",
                                "showUnderline": false,
                                "renderAs": "tags",
                                "tagBackground": "#F1F1F1",
                                "tagTextColor": "#2C2C2C",
                                "tagBorderRadius": 3,
                                "tagFontSize": 10,
                                "spacingAfter": 12
                            }
                        },
                        {
                            "type": "LANGUAGES",
                            "label": "Languages",
                            "enabled": true,
                            "order": 5,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": false,
                                "renderAs": "inline",
                                "spacingAfter": 10
                            }
                        }
                    ],
                    "twoColumn": {
                        "enabled": false,
                        "splitRatio": 0.35,
                        "mainSections": ["SUMMARY", "EXPERIENCE", "EDUCATION"],
                        "sidebarSections": ["SKILLS", "LANGUAGES", "CERTIFICATIONS"],
                        "sidebarBackground": "#F4F6FA",
                        "sidebarPadding": 16
                    }
                }
                """;

        private static final String TWO_COLUMN_EXECUTIVE_CONFIG = """
                {
                    "font": {
                        "heading": "Merriweather",
                        "body": "Open Sans",
                        "mono": "JetBrains Mono",
                        "baseSize": 11,
                        "lineHeight": 1.5
                    },
                    "colors": {
                        "accent": "#8B0000",
                        "headingText": "#111111",
                        "bodyText": "#333333",
                        "mutedText": "#666666",
                        "divider": "#CCCCCC",
                        "background": "#FFFFFF",
                        "sidebarBackground": "#F9F4F4"
                    },
                    "page": {
                        "marginTop": 34,
                        "marginBottom": 34,
                        "marginLeft": 44,
                        "marginRight": 44,
                        "layout": "two-column"
                    },
                    "header": {
                        "nameSize": 27,
                        "nameBold": true,
                        "nameColor": "#111111",
                        "subtitleSize": 12,
                        "subtitleColor": "#555555",
                        "contactSize": 10,
                        "contactLayout": "inline",
                        "showDivider": true,
                        "dividerColor": "#8B0000",
                        "dividerWeight": 2
                    },
                    "sections": [
                        {
                            "type": "SUMMARY",
                            "label": "Executive Summary",
                            "enabled": true,
                            "order": 1,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": true,
                                "labelColor": "#8B0000",
                                "showUnderline": true,
                                "underlineColor": "#8B0000",
                                "underlineWeight": 1.5,
                                "bodySize": 11,
                                "bodyColor": "#333333",
                                "spacingAfter": 12
                            }
                        },
                        {
                            "type": "EXPERIENCE",
                            "label": "Leadership Experience",
                            "enabled": true,
                            "order": 2,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": true,
                                "labelColor": "#8B0000",
                                "showUnderline": true,
                                "underlineColor": "#8B0000",
                                "underlineWeight": 1.5,
                                "entryTitleSize": 12,
                                "entryTitleBold": true,
                                "entryTitleColor": "#111111",
                                "entrySubtitleSize": 11,
                                "entrySubtitleItalic": true,
                                "entrySubtitleColor": "#444444",
                                "entryDateSize": 10,
                                "entryDateColor": "#666666",
                                "bulletSize": 10,
                                "bulletColor": "#333333",
                                "bulletIndent": 14,
                                "spacingAfter": 12
                            }
                        },
                        {
                            "type": "EDUCATION",
                            "label": "Education",
                            "enabled": true,
                            "order": 3,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": true,
                                "labelColor": "#8B0000",
                                "showUnderline": true,
                                "underlineColor": "#8B0000",
                                "underlineWeight": 1.5,
                                "entryTitleSize": 12,
                                "entryTitleBold": true,
                                "entrySubtitleItalic": true,
                                "spacingAfter": 12
                            }
                        },
                        {
                            "type": "SKILLS",
                            "label": "Core Skills",
                            "enabled": true,
                            "order": 4,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": true,
                                "labelColor": "#8B0000",
                                "showUnderline": false,
                                "renderAs": "tags",
                                "tagBackground": "#F3E9E9",
                                "tagTextColor": "#8B0000",
                                "tagBorderRadius": 4,
                                "tagFontSize": 10,
                                "spacingAfter": 12
                            }
                        },
                        {
                            "type": "LANGUAGES",
                            "label": "Languages",
                            "enabled": true,
                            "order": 5,
                            "style": {
                                "labelSize": 12,
                                "labelBold": true,
                                "labelUppercase": true,
                                "renderAs": "inline",
                                "spacingAfter": 10
                            }
                        }
                    ],
                    "twoColumn": {
                        "enabled": true,
                        "splitRatio": 0.32,
                        "mainSections": ["SUMMARY", "EXPERIENCE", "EDUCATION"],
                        "sidebarSections": ["SKILLS", "LANGUAGES", "CERTIFICATIONS"],
                        "sidebarBackground": "#F9F4F4",
                        "sidebarPadding": 16
                    }
                }
                """;

    public TemplateServiceImpl(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedDefaultTemplates() {
        if (templateRepository.count() > 0) {
            return;
        }

        Template classic = buildSeedTemplate(
                "Classic Professional",
                "Timeless typography with a confident accent.",
                CLASSIC_PROFESSIONAL_CONFIG,
                Template.Category.PROFESSIONAL,
                "#1F3A6E",
                "Playfair Display",
                "classic"
        );

        Template modern = buildSeedTemplate(
                "Modern Minimal",
                "Minimalist layout with strong typographic hierarchy.",
                MODERN_MINIMAL_CONFIG,
                Template.Category.MINIMALIST,
                "#2C2C2C",
                "Inter",
                "minimal"
        );

        Template executive = buildSeedTemplate(
                "Two-Column Executive",
                "Executive two-column layout with refined accents.",
                TWO_COLUMN_EXECUTIVE_CONFIG,
                Template.Category.PROFESSIONAL,
                "#8B0000",
                "Merriweather",
                "executive"
        );

        templateRepository.saveAll(List.of(classic, modern, executive));
    }

    @Override
    public List<Template> getAllActiveTemplates() {
        return templateRepository.findByIsActiveTrue();
    }

    @Override
    public List<Template> getFreeTemplates() {
        return templateRepository.findByIsActiveTrueAndIsPremiumFalse();
    }

    @Override
    public List<Template> getPremiumTemplates() {
        return templateRepository.findByIsActiveTrueAndIsPremiumTrue();
    }

    @Override
    public List<Template> getTemplatesByCategory(String categoryStr) {
        Template.Category category = Template.Category.valueOf(categoryStr.toUpperCase());
        return templateRepository.findByIsActiveTrueAndCategory(category);
    }

    @Override
    public List<Template> getPopularTemplates() {
        return templateRepository.findByIsActiveTrueOrderByUsageCountDesc();
    }

    @Override
    public Template getTemplateById(Integer id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
    }

    @Override
    @Transactional
    public Template createTemplate(TemplateRequest request, String role) {
        checkAdminAccess(role);
        String htmlLayout = normalizeHtmlLayout(request.getHtmlLayout());
        String cssStyles = normalizeCssStyles(request.getCssStyles());
        Template template = new Template(
                request.getName(),
                request.getDescription(),
                request.getThumbnailUrl(),
                htmlLayout,
                cssStyles,
                request.getCategory(),
                request.getIsPremium()
        );
        
        // Map aesthetic metadata
        template.setColorScheme(request.getColorScheme());
        template.setFontFamily(request.getFontFamily());
        template.setLayout(request.getLayout());
        template.setHasPhoto(request.getHasPhoto() != null ? request.getHasPhoto() : false);
        template.setHasSkillBars(request.getHasSkillBars() != null ? request.getHasSkillBars() : false);
        template.setPreviewData(request.getPreviewData());
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }
        if (request.getLayoutConfig() != null) {
            template.setLayoutConfig(request.getLayoutConfig());
        }
        
        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public Template updateTemplate(Integer id, TemplateRequest request, String role) {
        checkAdminAccess(role);
        Template template = getTemplateById(id);
        
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getHtmlLayout() != null) {
            template.setHtmlLayout(normalizeHtmlLayout(request.getHtmlLayout()));
        }
        if (request.getCssStyles() != null) {
            template.setCssStyles(normalizeCssStyles(request.getCssStyles()));
        }
        
        if (request.getCategory() != null) {
            template.setCategory(request.getCategory());
        }
        if (request.getIsPremium() != null) {
            template.setIsPremium(request.getIsPremium());
        }

        // Update aesthetic metadata
        template.setColorScheme(request.getColorScheme());
        template.setFontFamily(request.getFontFamily());
        template.setLayout(request.getLayout());
        template.setHasPhoto(request.getHasPhoto() != null ? request.getHasPhoto() : template.getHasPhoto());
        template.setHasSkillBars(request.getHasSkillBars() != null ? request.getHasSkillBars() : template.getHasSkillBars());
        template.setPreviewData(request.getPreviewData());
        if (request.getIsActive() != null) {
            template.setIsActive(request.getIsActive());
        }
        if (request.getLayoutConfig() != null) {
            template.setLayoutConfig(request.getLayoutConfig());
        }

        return templateRepository.save(template);
    }

    @Override
    @Transactional
    public void deactivateTemplate(Integer id, String role) {
        checkAdminAccess(role);
        Template template = getTemplateById(id);
        template.setIsActive(false);
        templateRepository.save(template);
    }

    @Override
    @Transactional
    public void incrementUsage(Integer id) {
        Template template = getTemplateById(id);
        template.setUsageCount(template.getUsageCount() + 1);
        templateRepository.save(template);
    }

    private void checkAdminAccess(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new RuntimeException("Access Denied: Admin role required");
        }
    }

    private Template buildSeedTemplate(String name, String description, String layoutConfig, Template.Category category,
                                       String colorScheme, String fontFamily, String layout) {
        Template template = new Template(
                name,
                description,
                null,
                DEFAULT_HTML_LAYOUT,
                DEFAULT_CSS_STYLES,
                category,
                false
        );
        template.setLayoutConfig(layoutConfig);
        template.setIsActive(true);
        template.setColorScheme(colorScheme);
        template.setFontFamily(fontFamily);
        template.setLayout(layout);
        template.setPreviewData(null);
        return template;
    }

    private String normalizeHtmlLayout(String htmlLayout) {
        if (htmlLayout == null || htmlLayout.isBlank()) {
            return DEFAULT_HTML_LAYOUT;
        }
        return htmlLayout;
    }

    private String normalizeCssStyles(String cssStyles) {
        if (cssStyles == null || cssStyles.isBlank()) {
            return DEFAULT_CSS_STYLES;
        }
        return cssStyles;
    }
}
