package com.resumade.template.dto;

import com.resumade.template.entity.Template;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {
    private Integer templateId;
    private String name;
    private String description;
    private String thumbnailUrl;
    private String htmlLayout;
    private String cssStyles;
    private String layoutConfig;
    private Template.Category category;
    private Boolean isPremium;
    private Boolean isActive;
    private Integer usageCount;
    private String colorScheme;
    private String fontFamily;
    private String layout;
    private Boolean hasPhoto;
    private Boolean hasSkillBars;
    private String previewData;
    private LocalDateTime createdAt;
}
