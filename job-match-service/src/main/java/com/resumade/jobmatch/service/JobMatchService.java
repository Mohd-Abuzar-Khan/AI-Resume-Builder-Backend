package com.resumade.jobmatch.service;

import com.resumade.jobmatch.entity.JobMatch;
import java.util.List;

public interface JobMatchService {
    List<JobMatch> fetchJobsFromLinkedIn(Integer userId, String title, String location);

    List<JobMatch> fetchJobsFromNaukri(Integer userId, String title, String location);

    List<JobMatch> searchJobs(Integer userId, String title, String location, String country, Integer page);

    JobMatch analyzeJobFit(Integer userId, Integer resumeId, Long matchId, String authToken);

    void toggleBookmark(Long matchId);

    List<JobMatch> getUserHistory(Integer userId);

    List<JobMatch> getBookmarks(Integer userId);

    java.util.Map<String, Object> testJooble(String title, String location);
}
