package com.resumade.template.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "templates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    @Builder.Default
    private Category category = Category.MODERN;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPremium = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    private String colorScheme;
    private String fontFamily;
    private String layout;
    
    @Builder.Default
    private Boolean hasPhoto = false;
    
    @Builder.Default
    private Boolean hasSkillBars = false;

    @Column(columnDefinition = "TEXT")
    private String previewData;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Manual constructor for backward compatibility with seeding code if needed, 
    // though @AllArgsConstructor + @Builder covers it.
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

    public enum Category { PROFESSIONAL, CREATIVE, MODERN, MINIMALIST, ATS_OPTIMISED }
}
