package com.resumade.template.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "templates")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer templateId;

    @Column(nullable = false)
    private String name;

    private String description;

    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String htmlLayout;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String cssStyles;

    @Column(columnDefinition = "TEXT")
    private String layoutConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category = Category.MODERN;

    @Column(nullable = false)
    private Boolean isPremium = false;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer usageCount = 0;

    // Aesthetic Metadata Fields
    private String colorScheme;
    private String fontFamily;
    private String layout; // e.g. "classic", "modern-split", "minimal"
    private Boolean hasPhoto = false;
    private Boolean hasSkillBars = false;

    @Column(columnDefinition = "TEXT")
    private String previewData; // JSON string for dummy data in live editor

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Template() {}

    public Template(String name, String description, String thumbnailUrl, String htmlLayout, String cssStyles, Category category, Boolean isPremium) {
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.htmlLayout = htmlLayout;
        this.cssStyles = cssStyles;
        this.category = category != null ? category : Category.MODERN;
        this.isPremium = isPremium != null ? isPremium : false;
        this.isActive = true;
        this.usageCount = 0;
    }

    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }
    
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

    public String getLayoutConfig() { return layoutConfig; }
    public void setLayoutConfig(String layoutConfig) { this.layoutConfig = layoutConfig; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public Boolean getIsPremium() { return isPremium; }
    public void setIsPremium(Boolean isPremium) { this.isPremium = isPremium; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

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
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public enum Category { PROFESSIONAL, CREATIVE, MODERN, MINIMALIST, ATS_OPTIMISED }
}
