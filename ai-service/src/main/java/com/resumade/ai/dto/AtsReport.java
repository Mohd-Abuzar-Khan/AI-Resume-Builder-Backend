package com.resumade.ai.dto;

import java.util.List;

public class AtsReport {
    private int score;
    private ScoreBreakdown breakdown;
    private List<String> keywordsFound;
    private List<String> keywordsMissing;
    private List<SuggestedAction> suggestions;
    private String verdict;

    public AtsReport() {}

    public AtsReport(int score, ScoreBreakdown breakdown, List<String> keywordsFound,
            List<String> keywordsMissing, List<SuggestedAction> suggestions, String verdict) {
        this.score = score;
        this.breakdown = breakdown;
        this.keywordsFound = keywordsFound;
        this.keywordsMissing = keywordsMissing;
        this.suggestions = suggestions;
        this.verdict = verdict;
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public ScoreBreakdown getBreakdown() { return breakdown; }
    public void setBreakdown(ScoreBreakdown breakdown) { this.breakdown = breakdown; }

    public List<String> getKeywordsFound() { return keywordsFound; }
    public void setKeywordsFound(List<String> keywordsFound) { this.keywordsFound = keywordsFound; }

    public List<String> getKeywordsMissing() { return keywordsMissing; }
    public void setKeywordsMissing(List<String> keywordsMissing) { this.keywordsMissing = keywordsMissing; }

    public List<SuggestedAction> getSuggestions() { return suggestions; }
    public void setSuggestions(List<SuggestedAction> suggestions) { this.suggestions = suggestions; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }

    public static AtsReportBuilder builder() {
        return new AtsReportBuilder();
    }

    public static class AtsReportBuilder {
        private int score;
        private ScoreBreakdown breakdown;
        private List<String> keywordsFound;
        private List<String> keywordsMissing;
        private List<SuggestedAction> suggestions;
        private String verdict;

        public AtsReportBuilder score(int score) { this.score = score; return this; }
        public AtsReportBuilder breakdown(ScoreBreakdown breakdown) { this.breakdown = breakdown; return this; }
        public AtsReportBuilder keywordsFound(List<String> keywordsFound) { this.keywordsFound = keywordsFound; return this; }
        public AtsReportBuilder keywordsMissing(List<String> keywordsMissing) { this.keywordsMissing = keywordsMissing; return this; }
        public AtsReportBuilder suggestions(List<SuggestedAction> suggestions) { this.suggestions = suggestions; return this; }
        public AtsReportBuilder verdict(String verdict) { this.verdict = verdict; return this; }
        public AtsReport build() {
            return new AtsReport(score, breakdown, keywordsFound, keywordsMissing, suggestions, verdict);
        }
    }

    public static class ScoreBreakdown {
        private CategoryScore keywordMatch;
        private CategoryScore experienceRelevance;
        private CategoryScore quantifiedAchievements;
        private CategoryScore formatReadability;
        private CategoryScore summaryAlignment;

        public ScoreBreakdown() {}

        public ScoreBreakdown(CategoryScore keywordMatch, CategoryScore experienceRelevance,
                CategoryScore quantifiedAchievements, CategoryScore formatReadability,
                CategoryScore summaryAlignment) {
            this.keywordMatch = keywordMatch;
            this.experienceRelevance = experienceRelevance;
            this.quantifiedAchievements = quantifiedAchievements;
            this.formatReadability = formatReadability;
            this.summaryAlignment = summaryAlignment;
        }

        public CategoryScore getKeywordMatch() { return keywordMatch; }
        public void setKeywordMatch(CategoryScore keywordMatch) { this.keywordMatch = keywordMatch; }

        public CategoryScore getExperienceRelevance() { return experienceRelevance; }
        public void setExperienceRelevance(CategoryScore experienceRelevance) { this.experienceRelevance = experienceRelevance; }

        public CategoryScore getQuantifiedAchievements() { return quantifiedAchievements; }
        public void setQuantifiedAchievements(CategoryScore quantifiedAchievements) { this.quantifiedAchievements = quantifiedAchievements; }

        public CategoryScore getFormatReadability() { return formatReadability; }
        public void setFormatReadability(CategoryScore formatReadability) { this.formatReadability = formatReadability; }

        public CategoryScore getSummaryAlignment() { return summaryAlignment; }
        public void setSummaryAlignment(CategoryScore summaryAlignment) { this.summaryAlignment = summaryAlignment; }
    }

    public static class CategoryScore {
        private int score;
        private int maxScore;
        private Double matchRate;

        public CategoryScore() {}

        public CategoryScore(int score, int maxScore, Double matchRate) {
            this.score = score;
            this.maxScore = maxScore;
            this.matchRate = matchRate;
        }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public int getMaxScore() { return maxScore; }
        public void setMaxScore(int maxScore) { this.maxScore = maxScore; }

        public Double getMatchRate() { return matchRate; }
        public void setMatchRate(Double matchRate) { this.matchRate = matchRate; }
    }

    public static class SuggestedAction {
        private String priority;
        private String category;
        private String action;

        public SuggestedAction() {}

        public SuggestedAction(String priority, String category, String action) {
            this.priority = priority;
            this.category = category;
            this.action = action;
        }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }
}
