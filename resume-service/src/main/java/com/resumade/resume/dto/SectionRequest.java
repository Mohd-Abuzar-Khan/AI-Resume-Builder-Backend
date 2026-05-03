package com.resumade.resume.dto;

import com.resumade.resume.entity.ResumeSection.SectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SectionRequest {

    @NotNull(message = "Section Type is required")
    private SectionType sectionType;

    @NotBlank(message = "Title is required")
    private String title;

    private String content;

    private Integer displayOrder;

    public SectionType getSectionType() { return sectionType; }
    public void setSectionType(SectionType sectionType) { this.sectionType = sectionType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
