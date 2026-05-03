package com.resumade.resume.service;

import com.resumade.resume.entity.Resume;
import com.resumade.resume.entity.ResumeSection;
import com.resumade.resume.exception.ResourceNotFoundException;
import com.resumade.resume.repository.ResumeRepository;
import com.resumade.resume.repository.ResumeSectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResumeServiceTest {

    private ResumeService resumeService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ResumeSectionRepository sectionRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resumeService = new ResumeServiceImpl(resumeRepository, sectionRepository);
    }

    @Test
    void duplicateResume_CoppiesAllSections() {
        Integer originalId = 1;
        Integer userId = 100;
        
        Resume original = new Resume(50, "Original", "Developer", 1);
        original.setIsPublic(true);
        ResumeSection section1 = new ResumeSection(original, ResumeSection.SectionType.EXPERIENCE, "Work", "Content", 0);
        original.setSections(Arrays.asList(section1));

        when(resumeRepository.findById(originalId)).thenReturn(Optional.of(original));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setResumeId(2);
            return r;
        });
        when(resumeRepository.findById(2)).thenAnswer(inv -> {
            Resume r = new Resume(userId, "Original (Copy)", "Developer", 1);
            r.setResumeId(2);
            return Optional.of(r);
        });

        Resume result = resumeService.duplicateResume(originalId, userId, "PREMIUM");

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertTrue(result.getTitle().contains("(Copy)"));
        
        verify(resumeRepository, times(1)).save(any(Resume.class));
        verify(sectionRepository, times(1)).save(any(ResumeSection.class));
    }

    @Test
    void searchPublicResumes_ReturnsFilteredResults() {
        String query = "Architect";
        when(resumeRepository.searchPublicResumes(query)).thenReturn(Arrays.asList(new Resume()));

        var results = resumeService.getPublicResumes(query);

        assertEquals(1, results.size());
        verify(resumeRepository).searchPublicResumes(query);
    }
}
