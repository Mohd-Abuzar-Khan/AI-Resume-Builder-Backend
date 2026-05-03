package com.resumade.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ResumeRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String targetJobTitle;

    @NotNull(message = "Template ID is required")
    private Integer templateId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTargetJobTitle() { return targetJobTitle; }
    public void setTargetJobTitle(String targetJobTitle) { this.targetJobTitle = targetJobTitle; }

    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }
}
