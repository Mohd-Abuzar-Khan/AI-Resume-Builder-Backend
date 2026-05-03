package com.resumade.resume.service;

import com.resumade.resume.dto.ResumeRequest;
import com.resumade.resume.dto.SectionOrderRequest;
import com.resumade.resume.dto.SectionRequest;
import com.resumade.resume.entity.Resume;
import com.resumade.resume.entity.ResumeSection;
import com.resumade.resume.exception.QuotaExceededException;
import com.resumade.resume.exception.ResourceNotFoundException;
import com.resumade.resume.exception.UnauthorizedAccessException;
import com.resumade.resume.repository.ResumeRepository;
import com.resumade.resume.repository.ResumeSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeSectionRepository sectionRepository;

    public ResumeServiceImpl(ResumeRepository resumeRepository, ResumeSectionRepository sectionRepository) {
        this.resumeRepository = resumeRepository;
        this.sectionRepository = sectionRepository;
    }

    @Override
    @Transactional
    public Resume createResume(Integer userId, String plan, ResumeRequest request) {
        // Enforce quota limits
        if ("FREE".equalsIgnoreCase(plan)) {
            long count = resumeRepository.countByUserId(userId);
            if (count >= 3) {
                throw new QuotaExceededException("Free plan users can only create 3 resumes. Please upgrade to create more.");
            }
        }

        Resume resume = new Resume(userId, request.getTitle(), request.getTargetJobTitle(), request.getTemplateId());
        return resumeRepository.save(resume);
    }

    @Override
    public Resume getResumeById(Integer resumeId, Integer userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        
        if (!resume.getUserId().equals(userId) && !resume.getIsPublic()) {
            throw new UnauthorizedAccessException("You do not have permission to view this resume");
        }
        
        // If public and viewed by someone else, increment view count
        if (resume.getIsPublic() && !resume.getUserId().equals(userId)) {
            resume.setViewCount(resume.getViewCount() + 1);
            resumeRepository.save(resume);
        }
        
        return resume;
    }

    @Override
    public List<Resume> getUserResumes(Integer userId) {
        return resumeRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    public List<Resume> getPublicResumes(String query) {
        if (query != null && !query.isEmpty()) {
            return resumeRepository.searchPublicResumes(query);
        }
        return resumeRepository.findByIsPublicTrueOrderByViewCountDesc();
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
    public Resume updateResume(Integer resumeId, Integer userId, ResumeRequest request) {
        Resume resume = getResumeForUser(resumeId, userId);
        resume.setTitle(request.getTitle());
        resume.setTargetJobTitle(request.getTargetJobTitle());
        resume.setTemplateId(request.getTemplateId());
        return resumeRepository.save(resume);
    }

    @Override
    @Transactional
    public void deleteResume(Integer resumeId, Integer userId) {
        Resume resume = getResumeForUser(resumeId, userId);
        resumeRepository.delete(resume);
    }

    @Override
    @Transactional
    public Resume duplicateResume(Integer resumeId, Integer userId, String plan) {
        // Enforce quota
        if ("FREE".equalsIgnoreCase(plan)) {
            long count = resumeRepository.countByUserId(userId);
            if (count >= 3) {
                throw new QuotaExceededException("Free plan users can only create 3 resumes. Please upgrade to create more.");
            }
        }

        Resume original = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));

        // Allow duplication if user is owner OR it's a public resume
        if (!original.getUserId().equals(userId) && !original.getIsPublic()) {
            throw new UnauthorizedAccessException("You do not have permission to duplicate this resume");
        }

        Resume copy = new Resume(userId, original.getTitle() + " (Copy)", original.getTargetJobTitle(), original.getTemplateId());
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
                    section.getDisplayOrder()
            );
            sectionCopy.setIsVisible(section.getIsVisible());
            sectionRepository.save(sectionCopy);
        }
        
        return resumeRepository.findById(savedCopy.getResumeId()).orElseThrow();
    }

    @Override
    @Transactional
    public Resume publishResume(Integer resumeId, Integer userId, boolean isPublic, String ownerName, String ownerAvatar) {
        Resume resume = getResumeForUser(resumeId, userId);
        resume.setIsPublic(isPublic);
        resume.setOwnerName(ownerName);
        resume.setOwnerAvatar(ownerAvatar);
        
        if (isPublic) {
            resume.setStatus(Resume.Status.PUBLISHED);
        } else {
            resume.setStatus(Resume.Status.COMPLETE);
        }
        return resumeRepository.save(resume);
    }

    // Section Methods
    
    @Override
    @Transactional
    public ResumeSection addSection(Integer resumeId, Integer userId, SectionRequest request) {
        Resume resume = getResumeForUser(resumeId, userId);
        ResumeSection section = new ResumeSection(
                resume,
                request.getSectionType(),
                request.getTitle(),
                request.getContent(),
                request.getDisplayOrder() != null ? request.getDisplayOrder() : resume.getSections().size()
        );
        return sectionRepository.save(section);
    }

    @Override
    @Transactional
    public ResumeSection updateSection(Integer sectionId, Integer userId, SectionRequest request) {
        ResumeSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        
        if (!section.getResume().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied to section");
        }
        
        section.setTitle(request.getTitle());
        section.setContent(request.getContent());
        
        return sectionRepository.save(section);
    }

    @Override
    @Transactional
    public void deleteSection(Integer sectionId, Integer userId) {
        ResumeSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        
        if (!section.getResume().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied to section");
        }
        
        sectionRepository.delete(section);
    }

    @Override
    @Transactional
    public void reorderSections(Integer resumeId, Integer userId, List<SectionOrderRequest> reorderRequests) {
        Resume resume = getResumeForUser(resumeId, userId);
        
        for (SectionOrderRequest orderReq : reorderRequests) {
            sectionRepository.updateSectionOrder(orderReq.getSectionId(), orderReq.getOrder());
        }
    }

    @Override
    @Transactional
    public ResumeSection toggleSectionVisibility(Integer sectionId, Integer userId, boolean isVisible) {
        ResumeSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        
        if (!section.getResume().getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied to section");
        }
        
        section.setIsVisible(isVisible);
        return sectionRepository.save(section);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getAdminStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalResumes", resumeRepository.count());
        stats.put("publicResumes", resumeRepository.findByIsPublicTrueOrderByViewCountDesc().size()); // Simplified
        return stats;
    }

    private Resume getResumeForUser(Integer resumeId, Integer userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        
        if (!resume.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You do not have permission to modify this resume");
        }
        return resume;
    }
}
