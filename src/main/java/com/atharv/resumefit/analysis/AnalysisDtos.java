package com.atharv.resumefit.analysis;

import java.time.Instant;
import java.util.List;

public final class AnalysisDtos {

    private AnalysisDtos() {
    }

    public record AnalysisResponse(
            Long id,
            String resumeFileName,
            String jobTitle,
            int fitScore,
            double skillCoverage,
            double textSimilarity,
            String verdict,
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> extraSkills,
            List<String> suggestions,
            Instant createdAt) {

        static AnalysisResponse from(Analysis a) {
            return new AnalysisResponse(a.getId(), a.getResumeFileName(), a.getJobTitle(), a.getFitScore(),
                    a.getSkillCoverage(), a.getTextSimilarity(), a.getVerdict(), a.getMatchedSkills(),
                    a.getMissingSkills(), a.getExtraSkills(), a.getSuggestions(), a.getCreatedAt());
        }
    }

    public record AnalysisSummary(Long id, String jobTitle, String resumeFileName, int fitScore, String verdict,
                                  Instant createdAt) {

        static AnalysisSummary from(Analysis a) {
            return new AnalysisSummary(a.getId(), a.getJobTitle(), a.getResumeFileName(), a.getFitScore(),
                    a.getVerdict(), a.getCreatedAt());
        }
    }

    public record SkillCount(String skill, long count) {
    }

    public record StatsResponse(long totalAnalyses, double averageScore, List<SkillCount> topMissingSkills) {
    }
}
