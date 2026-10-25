package com.atharv.resumefit.engine;

import java.util.List;

public record MatchResult(
        int fitScore,
        double skillCoverage,
        double textSimilarity,
        String verdict,
        List<String> matchedSkills,
        List<String> missingSkills,
        List<String> extraSkills,
        List<String> suggestions) {
}
