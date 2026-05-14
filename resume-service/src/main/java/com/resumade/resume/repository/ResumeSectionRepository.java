package com.resumade.resume.repository;

import com.resumade.resume.entity.ResumeSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeSectionRepository extends JpaRepository<ResumeSection, Integer> {
    List<ResumeSection> findByResumeResumeIdOrderByDisplayOrderAsc(Integer resumeId);
    
    @Modifying
    @Query("UPDATE ResumeSection s SET s.displayOrder = :order WHERE s.sectionId = :sectionId")
    void updateSectionOrder(@Param("sectionId") Integer sectionId, @Param("order") Integer order);
}
