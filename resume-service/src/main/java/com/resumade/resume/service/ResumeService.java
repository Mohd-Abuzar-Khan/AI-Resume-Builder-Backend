package com.resumade.resume.service;

import com.resumade.resume.dto.ResumeRequest;
import com.resumade.resume.dto.ResumeResponse;
import com.resumade.resume.dto.SectionOrderRequest;
import com.resumade.resume.dto.SectionRequest;
import com.resumade.resume.dto.SectionResponse;

import java.util.List;
import java.util.Map;

public interface ResumeService {
    // Resume API
    ResumeResponse createResume(Integer userId, String plan, ResumeRequest request);
    ResumeResponse getResumeById(Integer resumeId, Integer userId);
    List<ResumeResponse> getUserResumes(Integer userId);
    List<ResumeResponse> getPublicResumes(String query);
    void incrementViewCount(Integer resumeId);
    ResumeResponse updateResume(Integer resumeId, Integer userId, ResumeRequest request);
    void deleteResume(Integer resumeId, Integer userId);
    ResumeResponse duplicateResume(Integer resumeId, Integer userId, String plan);
    ResumeResponse publishResume(Integer resumeId, Integer userId, boolean isPublic, String ownerName, String ownerAvatar);

    // Section API
    SectionResponse addSection(Integer resumeId, Integer userId, SectionRequest request);
    SectionResponse updateSection(Integer sectionId, Integer userId, SectionRequest request);
    void deleteSection(Integer sectionId, Integer userId);
    void reorderSections(Integer resumeId, Integer userId, List<SectionOrderRequest> reorderRequests);
    SectionResponse toggleSectionVisibility(Integer sectionId, Integer userId, boolean isVisible);
    
    // Admin
    Map<String, Object> getAdminStats();
}
