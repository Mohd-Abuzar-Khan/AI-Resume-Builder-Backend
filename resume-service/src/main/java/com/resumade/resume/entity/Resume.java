package com.resumade.resume.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer resumeId;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private String title;

    private String targetJobTitle;

    @Column(nullable = false)
    private Integer templateId;

    private Integer atsScore = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    @Column(nullable = false)
    private String language = "en";

    @Column(nullable = false)
    private Boolean isPublic = false;

    @Column(nullable = false)
    private Integer viewCount = 0;

    private String ownerName;

    private String ownerAvatar;

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ResumeSection> sections = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Resume() {}

    public Resume(Integer userId, String title, String targetJobTitle, Integer templateId) {
        this.userId = userId;
        this.title = title;
        this.targetJobTitle = targetJobTitle;
        this.templateId = templateId;
        this.status = Status.DRAFT;
        this.language = "en";
        this.isPublic = false;
        this.viewCount = 0;
    }

    public Integer getResumeId() { return resumeId; }
    public void setResumeId(Integer resumeId) { this.resumeId = resumeId; }
    
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getTargetJobTitle() { return targetJobTitle; }
    public void setTargetJobTitle(String targetJobTitle) { this.targetJobTitle = targetJobTitle; }
    
    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }
    
    public Integer getAtsScore() { return atsScore; }
    public void setAtsScore(Integer atsScore) { this.atsScore = atsScore; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerAvatar() { return ownerAvatar; }
    public void setOwnerAvatar(String ownerAvatar) { this.ownerAvatar = ownerAvatar; }

    public void incrementViewCount() {
        this.viewCount++;
    }
    
    public List<ResumeSection> getSections() { return sections; }
    public void setSections(List<ResumeSection> sections) { this.sections = sections; }

    public void addSection(ResumeSection section) {
        sections.add(section);
        section.setResume(this);
    }

    public void removeSection(ResumeSection section) {
        sections.remove(section);
        section.setResume(null);
    }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum Status { DRAFT, COMPLETE, PUBLISHED }
}
