package com.resumade.template.dto;

import com.resumade.template.entity.Template;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private String thumbnailUrl;

    @NotBlank(message = "HTML Layout is required")
    private String htmlLayout;

    @NotBlank(message = "CSS Styles is required")
    private String cssStyles;

    private Template.Category category;

    private Boolean isPremium;

    private Boolean isActive;

    private String layoutConfig;

    private String colorScheme;
    private String fontFamily;
    private String layout;
    private Boolean hasPhoto;
    private Boolean hasSkillBars;
    private String previewData;
}
