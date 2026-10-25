package com.atharv.resumefit.analysis;

import com.atharv.resumefit.analysis.AnalysisDtos.AnalysisResponse;
import com.atharv.resumefit.analysis.AnalysisDtos.AnalysisSummary;
import com.atharv.resumefit.analysis.AnalysisDtos.StatsResponse;
import com.atharv.resumefit.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService service;

    public AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse create(Authentication auth,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam("jobTitle") String jobTitle,
                                   @RequestParam("jobDescription") String jobDescription) {
        return service.create(auth.getName(), file, jobTitle, jobDescription);
    }

    @GetMapping
    public PageResponse<AnalysisSummary> list(Authentication auth,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return service.list(auth.getName(), page, size);
    }

    @GetMapping("/stats")
    public StatsResponse stats(Authentication auth) {
        return service.stats(auth.getName());
    }

    @GetMapping("/{id}")
    public AnalysisResponse get(Authentication auth, @PathVariable Long id) {
        return service.get(auth.getName(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable Long id) {
        service.delete(auth.getName(), id);
    }
}
