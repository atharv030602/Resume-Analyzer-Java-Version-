package com.atharv.resumefit.config;

import com.atharv.resumefit.engine.MatchEngine;
import com.atharv.resumefit.engine.SkillCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    @Bean
    SkillCatalog skillCatalog() {
        return SkillCatalog.loadDefault();
    }

    @Bean
    MatchEngine matchEngine(SkillCatalog catalog) {
        return new MatchEngine(catalog);
    }
}
