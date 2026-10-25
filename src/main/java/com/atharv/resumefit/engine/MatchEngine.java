package com.atharv.resumefit.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scores how well a resume fits a job description.
 *
 * fit = 75% skill coverage (JD skills found in resume / JD skills)
 *     + 25% text similarity (cosine of word counts, scaled so 0.5 counts as full marks)
 *
 * If the JD has no known skills, only text similarity is used.
 */
public final class MatchEngine {

    static final double SKILL_WEIGHT = 0.75;
    static final double TEXT_WEIGHT = 0.25;
    static final double SIMILARITY_FULL_MARKS = 0.5;
    static final int MAX_SUGGESTIONS = 8;

    private static final Pattern WORD = Pattern.compile("[a-z0-9][a-z0-9+#]*");
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "are", "with", "you", "your", "our", "will", "have", "has", "from", "that",
            "this", "who", "all", "can", "any", "not", "but", "such", "also", "etc", "their", "they", "them",
            "was", "were", "been", "being", "into", "out", "over", "per", "via", "use", "using", "used",
            "work", "working", "years", "year", "experience", "role", "team", "teams", "strong", "good",
            "ability", "able", "knowledge", "skills", "skill", "required", "preferred", "responsibilities",
            "should", "must", "including", "within", "across", "other", "more", "than", "its", "may", "job");

    private static final Map<String, String> TIPS = Map.ofEntries(
            Map.entry("Language", "Show %s in a real project and mention what you built with it."),
            Map.entry("Backend", "Add a project that uses %s and describe the problem it solved."),
            Map.entry("Database", "Mention %s with a concrete detail, like a schema you designed or a query you tuned."),
            Map.entry("Frontend", "List %s next to a project where you built a screen or a component."),
            Map.entry("DevOps", "Add %s to a project, for example a Dockerfile or a pipeline, and link the repo."),
            Map.entry("Cloud", "Deploy one small project on %s and write it on your resume."),
            Map.entry("Testing", "Write a few tests with %s and mention the coverage or bugs you caught."),
            Map.entry("AI/ML", "Build a small demo with %s and put the link on your resume."),
            Map.entry("Data", "Add a dashboard or analysis made with %s and say what decision it helped."),
            Map.entry("Practice", "Show how you used %s in college work or an internship."),
            Map.entry("Soft", "Give one short example that proves %s, with a result."));

    private final SkillCatalog catalog;

    public MatchEngine(SkillCatalog catalog) {
        this.catalog = catalog;
    }

    public MatchResult analyze(String resumeText, String jobDescription) {
        Set<String> resumeSkills = catalog.extract(resumeText);
        Set<String> jdSkills = catalog.extract(jobDescription);

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : jdSkills) {
            (resumeSkills.contains(skill) ? matched : missing).add(skill);
        }
        List<String> extra = new ArrayList<>();
        for (String skill : resumeSkills) {
            if (!jdSkills.contains(skill)) {
                extra.add(skill);
            }
        }

        double similarity = cosine(tokenCounts(resumeText), tokenCounts(jobDescription));
        double similarityScore = Math.min(1.0, similarity / SIMILARITY_FULL_MARKS);

        double coverage;
        double fit;
        if (jdSkills.isEmpty()) {
            coverage = 0.0;
            fit = similarityScore;
        } else {
            coverage = (double) matched.size() / jdSkills.size();
            fit = SKILL_WEIGHT * coverage + TEXT_WEIGHT * similarityScore;
        }
        int fitScore = (int) Math.round(Math.max(0, Math.min(1, fit)) * 100);

        return new MatchResult(
                fitScore,
                round2(coverage),
                round2(similarity),
                verdict(fitScore),
                matched, missing, extra,
                suggestions(missing, jdSkills.isEmpty()));
    }

    static String verdict(int score) {
        if (score >= 75) {
            return "STRONG";
        }
        if (score >= 50) {
            return "MODERATE";
        }
        return "WEAK";
    }

    private List<String> suggestions(List<String> missing, boolean noSkillsInJd) {
        List<String> out = new ArrayList<>();
        if (noSkillsInJd) {
            out.add("No known skills were found in this job description, so the score uses text similarity only. "
                    + "Paste the full job description for a better result.");
            return out;
        }
        for (String skill : missing) {
            if (out.size() >= MAX_SUGGESTIONS) {
                break;
            }
            String tip = TIPS.getOrDefault(catalog.categoryOf(skill), "Add %s to your resume if you have used it.");
            out.add(skill + ": " + String.format(tip, skill));
        }
        if (missing.isEmpty()) {
            out.add("All skills from the job description appear on your resume. Add numbers (users, speed, savings) to your bullet points.");
        }
        return out;
    }

    private static Map<String, Integer> tokenCounts(String text) {
        Map<String, Integer> counts = new HashMap<>();
        if (text == null) {
            return counts;
        }
        Matcher m = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) {
            String w = m.group();
            if (w.length() >= 3 && !STOP_WORDS.contains(w)) {
                counts.merge(w, 1, Integer::sum);
            }
        }
        return counts;
    }

    private static double cosine(Map<String, Integer> a, Map<String, Integer> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        double dot = 0;
        Set<String> keys = new LinkedHashSet<>(a.keySet());
        keys.retainAll(b.keySet());
        for (String k : keys) {
            dot += (double) a.get(k) * b.get(k);
        }
        double na = 0;
        for (int v : a.values()) {
            na += (double) v * v;
        }
        double nb = 0;
        for (int v : b.values()) {
            nb += (double) v * v;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
