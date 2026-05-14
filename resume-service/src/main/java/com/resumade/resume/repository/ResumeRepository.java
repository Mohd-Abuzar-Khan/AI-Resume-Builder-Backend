package com.resumade.resume.repository;

import com.resumade.resume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Integer> {
    List<Resume> findByUserId(Integer userId);
    List<Resume> findByUserIdOrderByUpdatedAtDesc(Integer userId);
    List<Resume> findByIsPublicTrueOrderByViewCountDesc();
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Resume r WHERE r.isPublic = true AND (" +
           "LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(r.targetJobTitle) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(r.ownerName) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Resume> searchPublicResumes(@org.springframework.data.repository.query.Param("q") String q);

    long countByUserId(Integer userId);
    long countByIsPublicTrue();
}
