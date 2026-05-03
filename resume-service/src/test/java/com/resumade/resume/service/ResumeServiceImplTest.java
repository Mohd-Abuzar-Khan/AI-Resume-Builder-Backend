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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ResumeSectionRepository sectionRepository;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    private Resume testResume;
    private ResumeSection testSection;

    @BeforeEach
    void setUp() {
        testResume = new Resume(1, "Software Engineer", "Senior Dev", 1);
        testResume.setResumeId(1);
        testResume.setIsPublic(false);
        testResume.setSections(new ArrayList<>());
        
        testSection = new ResumeSection(testResume, ResumeSection.SectionType.EXPERIENCE, "Exp", "content", 1);
        testSection.setSectionId(1);
    }

    // ─── Create Resume ───

    @Test
    void createResume_shouldSaveResume() {
        when(resumeRepository.countByUserId(1)).thenReturn(2L);
        when(resumeRepository.save(any())).thenReturn(testResume);
        
        ResumeRequest request = new ResumeRequest();
        request.setTitle("Dev");
        Resume result = resumeService.createResume(1, "FREE", request);
        
        assertNotNull(result);
        verify(resumeRepository).save(any());
    }

    @Test
    void createResume_shouldThrowQuotaExceededForFreePlan() {
        when(resumeRepository.countByUserId(1)).thenReturn(3L);
        ResumeRequest request = new ResumeRequest();
        
        assertThrows(QuotaExceededException.class, () -> resumeService.createResume(1, "FREE", request));
    }

    @Test
    void createResume_shouldAllowPremiumUnlimited() {
        when(resumeRepository.save(any())).thenReturn(testResume);
        ResumeRequest request = new ResumeRequest();
        
        Resume result = resumeService.createResume(1, "PREMIUM", request);
        assertNotNull(result);
    }

    // ─── Get Resume ───

    @Test
    void getResumeById_shouldReturnResumeForOwner() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        
        Resume result = resumeService.getResumeById(1, 1);
        assertEquals(1, result.getResumeId());
    }

    @Test
    void getResumeById_shouldThrowWhenUserIsNotOwnerAndNotPublic() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        
        assertThrows(UnauthorizedAccessException.class, () -> resumeService.getResumeById(1, 2));
    }

    @Test
    void getResumeById_shouldReturnPublicResumeAndIncrementViews() {
        testResume.setIsPublic(true);
        testResume.setViewCount(0);
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        
        Resume result = resumeService.getResumeById(1, 2);
        
        assertEquals(1, result.getResumeId());
        assertEquals(1, result.getViewCount());
        verify(resumeRepository).save(testResume);
    }

    @Test
    void getResumeById_shouldThrowNotFound() {
        when(resumeRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> resumeService.getResumeById(1, 1));
    }

    @Test
    void getUserResumes_shouldReturnList() {
        when(resumeRepository.findByUserIdOrderByUpdatedAtDesc(1)).thenReturn(Arrays.asList(testResume));
        List<Resume> result = resumeService.getUserResumes(1);
        assertEquals(1, result.size());
    }

    @Test
    void getPublicResumes_withQuery_shouldReturnList() {
        when(resumeRepository.searchPublicResumes("java")).thenReturn(Arrays.asList(testResume));
        List<Resume> result = resumeService.getPublicResumes("java");
        assertEquals(1, result.size());
    }

    @Test
    void getPublicResumes_withoutQuery_shouldReturnList() {
        when(resumeRepository.findByIsPublicTrueOrderByViewCountDesc()).thenReturn(Arrays.asList(testResume));
        List<Resume> result = resumeService.getPublicResumes(null);
        assertEquals(1, result.size());
    }

    @Test
    void incrementViewCount_shouldIncrement() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        resumeService.incrementViewCount(1);
        assertEquals(1, testResume.getViewCount());
        verify(resumeRepository).save(testResume);
    }

    // ─── Update Resume ───

    @Test
    void updateResume_shouldUpdateAndSave() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        when(resumeRepository.save(any())).thenReturn(testResume);
        
        ResumeRequest request = new ResumeRequest();
        request.setTitle("New Title");
        
        Resume result = resumeService.updateResume(1, 1, request);
        assertEquals("New Title", result.getTitle());
    }

    // ─── Delete Resume ───

    @Test
    void deleteResume_shouldDelete() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        resumeService.deleteResume(1, 1);
        verify(resumeRepository).delete(testResume);
    }

    // ─── Duplicate Resume ───

    @Test
    void duplicateResume_shouldCreateCopy() {
        testResume.getSections().add(testSection);
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        when(resumeRepository.countByUserId(1)).thenReturn(1L);
        
        Resume copied = new Resume(1, "Copy", "Job", 1);
        copied.setResumeId(2);
        when(resumeRepository.save(any(Resume.class))).thenReturn(copied);
        when(resumeRepository.findById(2)).thenReturn(Optional.of(copied));
        
        Resume result = resumeService.duplicateResume(1, 1, "FREE");
        
        assertNotNull(result);
        verify(sectionRepository).save(any(ResumeSection.class));
    }

    @Test
    void duplicateResume_shouldThrowQuotaExceeded() {
        when(resumeRepository.countByUserId(1)).thenReturn(3L);
        assertThrows(QuotaExceededException.class, () -> resumeService.duplicateResume(1, 1, "FREE"));
    }

    @Test
    void duplicateResume_shouldThrowUnauthorized() {
        when(resumeRepository.countByUserId(2)).thenReturn(1L);
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        assertThrows(UnauthorizedAccessException.class, () -> resumeService.duplicateResume(1, 2, "FREE"));
    }

    // ─── Publish Resume ───

    @Test
    void publishResume_shouldSetPublic() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        when(resumeRepository.save(any())).thenReturn(testResume);
        
        Resume result = resumeService.publishResume(1, 1, true, "Owner", "Avatar");
        
        assertTrue(result.getIsPublic());
        assertEquals(Resume.Status.PUBLISHED, result.getStatus());
    }

    @Test
    void publishResume_shouldSetCompleteWhenNotPublic() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        when(resumeRepository.save(any())).thenReturn(testResume);
        
        Resume result = resumeService.publishResume(1, 1, false, "Owner", "Avatar");
        
        assertFalse(result.getIsPublic());
        assertEquals(Resume.Status.COMPLETE, result.getStatus());
    }

    // ─── Sections ───

    @Test
    void addSection_shouldSaveSection() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        when(sectionRepository.save(any())).thenReturn(testSection);
        
        SectionRequest request = new SectionRequest();
        request.setSectionType(ResumeSection.SectionType.EDUCATION);
        
        ResumeSection result = resumeService.addSection(1, 1, request);
        assertNotNull(result);
    }

    @Test
    void updateSection_shouldUpdate() {
        when(sectionRepository.findById(1)).thenReturn(Optional.of(testSection));
        when(sectionRepository.save(any())).thenReturn(testSection);
        
        SectionRequest request = new SectionRequest();
        request.setTitle("New Title");
        
        ResumeSection result = resumeService.updateSection(1, 1, request);
        assertEquals("New Title", result.getTitle());
    }

    @Test
    void updateSection_shouldThrowUnauthorized() {
        when(sectionRepository.findById(1)).thenReturn(Optional.of(testSection));
        SectionRequest request = new SectionRequest();
        
        assertThrows(UnauthorizedAccessException.class, () -> resumeService.updateSection(1, 2, request));
    }

    @Test
    void deleteSection_shouldDelete() {
        when(sectionRepository.findById(1)).thenReturn(Optional.of(testSection));
        resumeService.deleteSection(1, 1);
        verify(sectionRepository).delete(testSection);
    }

    @Test
    void reorderSections_shouldUpdateOrder() {
        when(resumeRepository.findById(1)).thenReturn(Optional.of(testResume));
        SectionOrderRequest req1 = new SectionOrderRequest();
        req1.setSectionId(1);
        req1.setOrder(1);
        
        resumeService.reorderSections(1, 1, Arrays.asList(req1));
        
        verify(sectionRepository).updateSectionOrder(1, 1);
    }

    @Test
    void toggleSectionVisibility_shouldToggle() {
        when(sectionRepository.findById(1)).thenReturn(Optional.of(testSection));
        when(sectionRepository.save(any())).thenReturn(testSection);
        
        ResumeSection result = resumeService.toggleSectionVisibility(1, 1, false);
        
        assertFalse(result.getIsVisible());
    }

    @Test
    void getAdminStats_shouldReturnStats() {
        when(resumeRepository.count()).thenReturn(100L);
        when(resumeRepository.findByIsPublicTrueOrderByViewCountDesc()).thenReturn(Arrays.asList(testResume));
        
        java.util.Map<String, Object> stats = resumeService.getAdminStats();
        
        assertEquals(100L, stats.get("totalResumes"));
        assertEquals(1, stats.get("publicResumes"));
    }
}
