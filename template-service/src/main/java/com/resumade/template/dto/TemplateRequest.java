package com.resumade.template.dto;

import com.resumade.template.entity.Template;

import jakarta.validation.constraints.NotBlank;

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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    
    public String getHtmlLayout() { return htmlLayout; }
    public void setHtmlLayout(String htmlLayout) { this.htmlLayout = htmlLayout; }
    
    public String getCssStyles() { return cssStyles; }
    public void setCssStyles(String cssStyles) { this.cssStyles = cssStyles; }
    
    public Template.Category getCategory() { return category; }
    public void setCategory(Template.Category category) { this.category = category; }
    
    public Boolean getIsPremium() { return isPremium; }
    public void setIsPremium(Boolean isPremium) { this.isPremium = isPremium; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getLayoutConfig() { return layoutConfig; }
    public void setLayoutConfig(String layoutConfig) { this.layoutConfig = layoutConfig; }

    public String getColorScheme() { return colorScheme; }
    public void setColorScheme(String colorScheme) { this.colorScheme = colorScheme; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public String getLayout() { return layout; }
    public void setLayout(String layout) { this.layout = layout; }

    public Boolean getHasPhoto() { return hasPhoto; }
    public void setHasPhoto(Boolean hasPhoto) { this.hasPhoto = hasPhoto; }

    public Boolean getHasSkillBars() { return hasSkillBars; }
    public void setHasSkillBars(Boolean hasSkillBars) { this.hasSkillBars = hasSkillBars; }

    public String getPreviewData() { return previewData; }
    public void setPreviewData(String previewData) { this.previewData = previewData; }
}
