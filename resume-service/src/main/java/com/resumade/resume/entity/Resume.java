package com.resumade.resume.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Core resume entity — holds metadata, template reference, and cascades to ordered sections
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

    @Builder.Default
    private Integer atsScore = 0; // Populated by ai-service ATS check endpoint

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(nullable = false)
    @Builder.Default
    private String language = "en";

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = false; // Controls visibility in the public resume gallery

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    private String ownerName;

    private String ownerAvatar;

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC") // Sections rendered in user-defined order
    @Builder.Default
    private List<ResumeSection> sections = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Resume(Integer userId, String title, String targetJobTitle, Integer templateId) {
        this.userId = userId;
        this.title = title;
        this.targetJobTitle = targetJobTitle;
        this.templateId = templateId;
        this.status = Status.DRAFT;
        this.language = "en";
        this.isPublic = false;
        this.viewCount = 0;
        this.sections = new ArrayList<>();
    }

    // WARNING: Not atomic — concurrent reads may lose increments under load
    public void incrementViewCount() {
        this.viewCount++;
    }

    // Helper methods for bidirectional relationship — currently unused (sections managed via repository)
    public void addSection(ResumeSection section) {
        sections.add(section);
        section.setResume(this);
    }

    public void removeSection(ResumeSection section) {
        sections.remove(section);
        section.setResume(null);
    }

    public enum Status { DRAFT, COMPLETE, PUBLISHED }
}
