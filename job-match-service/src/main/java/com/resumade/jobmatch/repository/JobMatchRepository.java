package com.resumade.jobmatch.repository;

import com.resumade.jobmatch.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {
    List<JobMatch> findByUserIdOrderByMatchedAtDesc(Integer userId);
    List<JobMatch> findByResumeId(Integer resumeId);
    List<JobMatch> findByUserIdAndIsBookmarkedTrue(Integer userId);
}
