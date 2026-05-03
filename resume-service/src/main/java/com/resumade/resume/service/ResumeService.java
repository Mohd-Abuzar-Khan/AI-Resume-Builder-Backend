package com.resumade.resume.service;

import com.resumade.resume.dto.ResumeRequest;
import com.resumade.resume.dto.SectionOrderRequest;
import com.resumade.resume.dto.SectionRequest;
import com.resumade.resume.entity.Resume;
import com.resumade.resume.entity.ResumeSection;

import java.util.List;

public interface ResumeService {
    // Resume API
    Resume createResume(Integer userId, String plan, ResumeRequest request);
    Resume getResumeById(Integer resumeId, Integer userId);
    List<Resume> getUserResumes(Integer userId);
    List<Resume> getPublicResumes(String query);
    void incrementViewCount(Integer resumeId);
    Resume updateResume(Integer resumeId, Integer userId, ResumeRequest request);
    void deleteResume(Integer resumeId, Integer userId);
    Resume duplicateResume(Integer resumeId, Integer userId, String plan);
    Resume publishResume(Integer resumeId, Integer userId, boolean isPublic, String ownerName, String ownerAvatar);

    // Section API
    ResumeSection addSection(Integer resumeId, Integer userId, SectionRequest request);
    ResumeSection updateSection(Integer sectionId, Integer userId, SectionRequest request);
    void deleteSection(Integer sectionId, Integer userId);
    void reorderSections(Integer resumeId, Integer userId, List<SectionOrderRequest> reorderRequests);
    ResumeSection toggleSectionVisibility(Integer sectionId, Integer userId, boolean isVisible);
    
    // Admin
    java.util.Map<String, Object> getAdminStats();
}
