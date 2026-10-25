package com.atharv.resumefit.analysis;

import com.atharv.resumefit.analysis.AnalysisDtos.AnalysisResponse;
import com.atharv.resumefit.analysis.AnalysisDtos.AnalysisSummary;
import com.atharv.resumefit.analysis.AnalysisDtos.SkillCount;
import com.atharv.resumefit.analysis.AnalysisDtos.StatsResponse;
import com.atharv.resumefit.common.BadRequestException;
import com.atharv.resumefit.common.NotFoundException;
import com.atharv.resumefit.common.PageResponse;
import com.atharv.resumefit.common.UnauthorizedException;
import com.atharv.resumefit.engine.MatchEngine;
import com.atharv.resumefit.engine.MatchResult;
import com.atharv.resumefit.parser.DocumentTextExtractor;
import com.atharv.resumefit.user.User;
import com.atharv.resumefit.user.UserRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AnalysisService {

    static final int MIN_JD_LENGTH = 30;
    static final int MAX_JD_LENGTH = 20_000;
    static final int MAX_PAGE_SIZE = 50;

    private final AnalysisRepository analyses;
    private final UserRepository users;
    private final DocumentTextExtractor extractor;
    private final MatchEngine engine;

    public AnalysisService(AnalysisRepository analyses, UserRepository users, DocumentTextExtractor extractor,
                           MatchEngine engine) {
        this.analyses = analyses;
        this.users = users;
        this.extractor = extractor;
        this.engine = engine;
    }

    @Transactional
    public AnalysisResponse create(String email, MultipartFile file, String jobTitle, String jobDescription) {
        User user = currentUser(email);

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please upload your resume");
        }
        if (jobTitle == null || jobTitle.isBlank() || jobTitle.strip().length() > 200) {
            throw new BadRequestException("Job title is required (max 200 characters)");
        }
        if (jobDescription == null || jobDescription.strip().length() < MIN_JD_LENGTH
                || jobDescription.length() > MAX_JD_LENGTH) {
            throw new BadRequestException("Job description must be between " + MIN_JD_LENGTH + " and "
                    + MAX_JD_LENGTH + " characters");
        }

        String resumeText = extractor.extract(file);
        if (resumeText == null || resumeText.isBlank()) {
            throw new BadRequestException("No text found in the resume. Scanned or image-only PDFs are not supported.");
        }

        MatchResult r = engine.analyze(resumeText, jobDescription);
        Analysis saved = analyses.save(new Analysis(
                user, cleanFileName(file.getOriginalFilename()), jobTitle.strip(), jobDescription.strip(),
                r.fitScore(), r.skillCoverage(), r.textSimilarity(), r.verdict(),
                r.matchedSkills(), r.missingSkills(), r.extraSkills(), r.suggestions()));
        return AnalysisResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AnalysisSummary> list(String email, int page, int size) {
        User user = currentUser(email);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(analyses.findByUserId(user.getId(), pageable).map(AnalysisSummary::from));
    }

    @Transactional(readOnly = true)
    public AnalysisResponse get(String email, Long id) {
        User user = currentUser(email);
        return analyses.findByIdAndUserId(id, user.getId())
                .map(AnalysisResponse::from)
                .orElseThrow(() -> new NotFoundException("Analysis not found"));
    }

    @Transactional
    public void delete(String email, Long id) {
        User user = currentUser(email);
        Analysis a = analyses.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("Analysis not found"));
        analyses.delete(a);
    }

    @Transactional(readOnly = true)
    public StatsResponse stats(String email) {
        User user = currentUser(email);
        long total = analyses.countByUserId(user.getId());
        Double avg = analyses.averageScore(user.getId());

        Map<String, Long> counts = new HashMap<>();
        for (Analysis a : analyses.findTop50ByUserIdOrderByCreatedAtDesc(user.getId())) {
            for (String skill : a.getMissingSkills()) {
                counts.merge(skill, 1L, Long::sum);
            }
        }
        List<SkillCount> top = counts.entrySet().stream()
                .map(e -> new SkillCount(e.getKey(), e.getValue()))
                .sorted(Comparator.<SkillCount>comparingLong(SkillCount::count).reversed().thenComparing(SkillCount::skill))
                .limit(5)
                .toList();
        double average = avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;
        return new StatsResponse(total, average, top);
    }

    private User currentUser(String email) {
        return users.findByEmail(email).orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
    }

    private static String cleanFileName(String original) {
        if (original == null || original.isBlank()) {
            return "resume";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).strip();
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
