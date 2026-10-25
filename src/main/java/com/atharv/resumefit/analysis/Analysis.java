package com.atharv.resumefit.analysis;

import com.atharv.resumefit.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "resume_file_name", nullable = false)
    private String resumeFileName;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "job_description", nullable = false, columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "fit_score", nullable = false)
    private int fitScore;

    @Column(name = "skill_coverage", nullable = false)
    private double skillCoverage;

    @Column(name = "text_similarity", nullable = false)
    private double textSimilarity;

    @Column(nullable = false)
    private String verdict;

    @Convert(converter = StringListConverter.class)
    @Column(name = "matched_skills", nullable = false, columnDefinition = "TEXT")
    private List<String> matchedSkills;

    @Convert(converter = StringListConverter.class)
    @Column(name = "missing_skills", nullable = false, columnDefinition = "TEXT")
    private List<String> missingSkills;

    @Convert(converter = StringListConverter.class)
    @Column(name = "extra_skills", nullable = false, columnDefinition = "TEXT")
    private List<String> extraSkills;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> suggestions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Analysis() {
    }

    public Analysis(User user, String resumeFileName, String jobTitle, String jobDescription, int fitScore,
                    double skillCoverage, double textSimilarity, String verdict, List<String> matchedSkills,
                    List<String> missingSkills, List<String> extraSkills, List<String> suggestions) {
        this.user = user;
        this.resumeFileName = resumeFileName;
        this.jobTitle = jobTitle;
        this.jobDescription = jobDescription;
        this.fitScore = fitScore;
        this.skillCoverage = skillCoverage;
        this.textSimilarity = textSimilarity;
        this.verdict = verdict;
        this.matchedSkills = matchedSkills;
        this.missingSkills = missingSkills;
        this.extraSkills = extraSkills;
        this.suggestions = suggestions;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getResumeFileName() { return resumeFileName; }
    public String getJobTitle() { return jobTitle; }
    public String getJobDescription() { return jobDescription; }
    public int getFitScore() { return fitScore; }
    public double getSkillCoverage() { return skillCoverage; }
    public double getTextSimilarity() { return textSimilarity; }
    public String getVerdict() { return verdict; }
    public List<String> getMatchedSkills() { return matchedSkills; }
    public List<String> getMissingSkills() { return missingSkills; }
    public List<String> getExtraSkills() { return extraSkills; }
    public List<String> getSuggestions() { return suggestions; }
    public Instant getCreatedAt() { return createdAt; }
}
