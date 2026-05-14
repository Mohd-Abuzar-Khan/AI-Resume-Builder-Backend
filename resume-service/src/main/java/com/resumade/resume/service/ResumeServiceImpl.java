package com.resumade.resume.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.resumade.resume.dto.ResumeRequest;
import com.resumade.resume.dto.ResumeResponse;
import com.resumade.resume.dto.SectionOrderRequest;
import com.resumade.resume.dto.SectionRequest;
import com.resumade.resume.dto.SectionResponse;
import com.resumade.resume.entity.Resume;
import com.resumade.resume.entity.ResumeSection;
import com.resumade.resume.exception.QuotaExceededException;
import com.resumade.resume.exception.ResourceNotFoundException;
import com.resumade.resume.exception.UnauthorizedAccessException;
import com.resumade.resume.repository.ResumeRepository;
import com.resumade.resume.repository.ResumeSectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
// Core resume business logic — enforces plan-based quotas, ownership checks,
// and section management
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeSectionRepository sectionRepository;

    @Override
    @Transactional
    public ResumeResponse createResume(Integer userId, String plan, ResumeRequest request) {
        checkResumeQuota(userId, plan);

        Resume resume = new Resume(userId, request.getTitle(), request.getTargetJobTitle(), request.getTemplateId());
        return mapToResponse(resumeRepository.save(resume));
    }

    @Override
    public ResumeResponse getResumeById(Integer resumeId, Integer userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        if (!resume.getUserId().equals(userId) && !resume.getIsPublic()) {
            throw new UnauthorizedAccessException("You do not have permission to view this resume");
        }

        // Non-atomic view count increment — consider using @Query UPDATE for atomicity
        if (resume.getIsPublic() && !resume.getUserId().equals(userId)) {
            resume.setViewCount(resume.getViewCount() + 1);
            resumeRepository.save(resume);
        }

        return mapToResponse(resume);
    }

    @Override
    public List<ResumeResponse> getUserResumes(Integer userId) {
        return resumeRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ResumeResponse> getPublicResumes(String query) {
        List<Resume> resumes;
        if (query != null && !query.isEmpty()) {
            resumes = resumeRepository.searchPublicResumes(query);
        } else {
            resumes = resumeRepository.findByIsPublicTrueOrderByViewCountDesc();
        }
        return resumes.stream()
                .map(this::mapToResponse)
            .toList();
    }

    @Override
    @Transactional
    public void incrementViewCount(Integer resumeId) {
        resumeRepository.findById(resumeId).ifPresent(resume -> {
            resume.incrementViewCount();
            resumeRepository.save(resume);
        });
    }

    @Override
    @Transactional
    public ResumeResponse updateResume(Integer resumeId, Integer userId, ResumeRequest request) {
        Resume resume = getResumeForUser(resumeId, userId);
        resume.setTitle(request.getTitle());
        resume.setTargetJobTitle(request.getTargetJobTitle());
        resume.setTemplateId(request.getTemplateId());
        return mapToResponse(resumeRepository.save(resume));
    }

    @Override
    @Transactional
    public void deleteResume(Integer resumeId, Integer userId) {
        Resume resume = getResumeForUser(resumeId, userId);
        resumeRepository.delete(resume);
    }

    @Override
    @Transactional
    public ResumeResponse duplicateResume(Integer resumeId, Integer userId, String plan) {
        checkResumeQuota(userId, plan);

        Resume original = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        // Cross-user duplication allowed for public resumes (community template
        // feature)
        if (!original.getUserId().equals(userId) && !original.getIsPublic()) {
            throw new UnauthorizedAccessException("You do not have permission to duplicate this resume");
        }

        Resume copy = new Resume(userId, original.getTitle() + " (Copy)", original.getTargetJobTitle(),
                original.getTemplateId());
        copy.setLanguage(original.getLanguage());
        copy.setStatus(Resume.Status.DRAFT);
        copy.setIsPublic(false);
        copy.setViewCount(0);

        Resume savedCopy = resumeRepository.save(copy);

        for (ResumeSection section : original.getSections()) {
            ResumeSection sectionCopy = new ResumeSection(
                    savedCopy,
                    section.getSectionType(),
                    section.getTitle(),
                    section.getContent(),
                    section.getDisplayOrder());
            sectionCopy.setIsVisible(section.getIsVisible());
            sectionRepository.save(sectionCopy);
        }

        return mapToResponse(resumeRepository.findById(savedCopy.getResumeId()).orElseThrow());
    }

    @Override
    @Transactional
    public ResumeResponse publishResume(Integer resumeId, Integer userId, boolean isPublic, String ownerName,
            String ownerAvatar) {
        Resume resume = getResumeForUser(resumeId, userId);
        resume.setIsPublic(isPublic);
        resume.setOwnerName(ownerName);
        resume.setOwnerAvatar(ownerAvatar);

        if (isPublic) {
            resume.setStatus(Resume.Status.PUBLISHED);
        } else {
            resume.setStatus(Resume.Status.COMPLETE);
        }
        return mapToResponse(resumeRepository.save(resume));
    }

    // Section Methods

    @Override
    @Transactional
    public SectionResponse addSection(Integer resumeId, Integer userId, SectionRequest request) {
        Resume resume = getResumeForUser(resumeId, userId);
        ResumeSection section = new ResumeSection(
                resume,
                request.getSectionType(),
                request.getTitle(),
                request.getContent(),
                request.getDisplayOrder() != null ? request.getDisplayOrder() : resume.getSections().size());
        return mapSectionToResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public SectionResponse updateSection(Integer sectionId, Integer userId, SectionRequest request) {
        ResumeSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        checkSectionOwnership(section, userId);

        section.setTitle(request.getTitle());
        section.setContent(request.getContent());

        return mapSectionToResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional
    public void deleteSection(Integer sectionId, Integer userId) {
        ResumeSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        checkSectionOwnership(section, userId);

        sectionRepository.delete(section);
    }

    @Override
    @Transactional
    public void reorderSections(Integer resumeId, Integer userId, List<SectionOrderRequest> reorderRequests) {
        getResumeForUser(resumeId, userId);

        for (SectionOrderRequest orderReq : reorderRequests) {
            sectionRepository.updateSectionOrder(orderReq.getSectionId(), orderReq.getOrder());
        }
    }

    @Override
    @Transactional
    public SectionResponse toggleSectionVisibility(Integer sectionId, Integer userId, boolean isVisible) {
        ResumeSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

        checkSectionOwnership(section, userId);

        section.setIsVisible(isVisible);
        return mapSectionToResponse(sectionRepository.save(section));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalResumes", resumeRepository.count());
        stats.put("publicResumes", resumeRepository.countByIsPublicTrue());
        return stats;
    }

    // Ownership guard — used by all mutating operations to prevent cross-user
    // access
    private Resume getResumeForUser(Integer resumeId, Integer userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        if (!resume.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You do not have permission to modify this resume");
        }
        return resume;
    }

    private void checkResumeQuota(Integer userId, String plan) {
        if ("FREE".equalsIgnoreCase(plan)) {
            long count = resumeRepository.countByUserId(userId);
            if (count >= 3) {
                throw new QuotaExceededException(
                        "Free plan users can only create 3 resumes. Please upgrade to create more.");
            }
        }
    }

    private void checkSectionOwnership(ResumeSection section, Integer userId) {
        if (!section.getResume().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied to section");
        }
    }

    private ResumeResponse mapToResponse(Resume resume) {
        return ResumeResponse.builder()
                .resumeId(resume.getResumeId())
                .userId(resume.getUserId())
                .title(resume.getTitle())
                .targetJobTitle(resume.getTargetJobTitle())
                .templateId(resume.getTemplateId())
                .atsScore(resume.getAtsScore())
                .status(resume.getStatus())
                .language(resume.getLanguage())
                .isPublic(resume.getIsPublic())
                .viewCount(resume.getViewCount())
                .ownerName(resume.getOwnerName())
                .ownerAvatar(resume.getOwnerAvatar())
                .sections(resume.getSections() == null ? new ArrayList<>()
                        : resume.getSections().stream()
                                .map(this::mapSectionToResponse)
                        .toList())
                .createdAt(resume.getCreatedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    private SectionResponse mapSectionToResponse(ResumeSection section) {
        return SectionResponse.builder()
                .sectionId(section.getSectionId())
                .sectionType(section.getSectionType())
                .title(section.getTitle())
                .content(section.getContent())
                .displayOrder(section.getDisplayOrder())
                .isVisible(section.getIsVisible())
                .aiGenerated(section.getAiGenerated())
                .createdAt(section.getCreatedAt())
                .build();
    }
}
