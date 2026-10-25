package com.atharv.resumefit.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MatchEngineTest {

    private final SkillCatalog catalog = SkillCatalog.loadDefault();
    private final MatchEngine engine = new MatchEngine(catalog);

    @Test
    void findsMultiWordSkillsAcrossLineBreaks() {
        assertThat(catalog.extract("Built APIs with Spring\n  Boot and MySQL")).contains("Spring Boot", "MySQL");
    }

    @Test
    void javaIsNotFoundInsideJavaScript() {
        assertThat(catalog.extract("I know JavaScript well")).contains("JavaScript").doesNotContain("Java");
    }

    @Test
    void handlesSymbolsInSkillNames() {
        assertThat(catalog.extract("C++, C# and Node.js")).contains("C++", "C#", "Node.js");
    }

    @Test
    void mySqlDoesNotCountAsSql() {
        assertThat(catalog.extract("Used MySQL daily")).contains("MySQL").doesNotContain("SQL");
    }

    @Test
    void splitsMatchedAndMissingSkills() {
        MatchResult r = engine.analyze(
                "Java developer with Spring Boot, MySQL and Docker.",
                "Looking for Java, Spring Boot, Kubernetes and AWS experience.");
        assertThat(r.matchedSkills()).containsExactly("Java", "Spring Boot");
        assertThat(r.missingSkills()).containsExactly("Kubernetes", "AWS");
        assertThat(r.extraSkills()).contains("MySQL", "Docker");
        assertThat(r.skillCoverage()).isEqualTo(0.5);
    }

    @Test
    void perfectMatchScoresStrong() {
        String text = "Java Spring Boot MySQL Docker REST APIs backend developer";
        MatchResult r = engine.analyze(text, text);
        assertThat(r.fitScore()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo("STRONG");
        assertThat(r.missingSkills()).isEmpty();
    }

    @Test
    void noOverlapScoresWeak() {
        MatchResult r = engine.analyze("Python and Django developer", "Need Java, Spring Boot and Kubernetes engineer");
        assertThat(r.fitScore()).isLessThan(50);
        assertThat(r.verdict()).isEqualTo("WEAK");
        assertThat(r.suggestions()).isNotEmpty();
    }

    @Test
    void jobDescriptionWithoutKnownSkillsGivesWarning() {
        MatchResult r = engine.analyze("Java developer", "We want a friendly person who enjoys cake");
        assertThat(r.suggestions().get(0)).contains("No known skills");
    }

    @Test
    void emptyResumeDoesNotCrash() {
        MatchResult r = engine.analyze("", "Java and Docker engineer");
        assertThat(r.fitScore()).isZero();
        assertThat(r.missingSkills()).containsExactly("Java", "Docker");
    }

    @Test
    void verdictBoundaries() {
        assertThat(MatchEngine.verdict(75)).isEqualTo("STRONG");
        assertThat(MatchEngine.verdict(74)).isEqualTo("MODERATE");
        assertThat(MatchEngine.verdict(50)).isEqualTo("MODERATE");
        assertThat(MatchEngine.verdict(49)).isEqualTo("WEAK");
    }
}
