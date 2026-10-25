package com.atharv.resumefit.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Dictionary of known skills. Pure Java, no Spring, so it is easy to unit test.
 * File format (one per line): Category|Canonical name|alias1,alias2,...
 */
public final class SkillCatalog {

    private record Skill(String category, String name, List<Pattern> patterns) {}

    private final List<Skill> skills = new ArrayList<>();
    private final Map<String, String> categoryByName = new HashMap<>();

    private SkillCatalog() {}

    public static SkillCatalog loadDefault() {
        try (InputStream in = SkillCatalog.class.getResourceAsStream("/skills.txt")) {
            if (in == null) {
                throw new IllegalStateException("skills.txt not found on classpath");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return fromLines(reader.lines().toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SkillCatalog fromLines(List<String> lines) {
        SkillCatalog catalog = new SkillCatalog();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", 3);
            if (parts.length < 2) {
                continue;
            }
            String category = parts[0].strip();
            String name = parts[1].strip();
            String aliasText = parts.length == 3 ? parts[2] : name;
            List<Pattern> patterns = new ArrayList<>();
            for (String alias : aliasText.split(",")) {
                String a = alias.strip();
                if (!a.isEmpty()) {
                    patterns.add(toPattern(a));
                }
            }
            catalog.skills.add(new Skill(category, name, patterns));
            catalog.categoryByName.put(name, category);
        }
        return catalog;
    }

    /** "Spring Boot" -> Spring\s+Boot, and the match must not touch letters, digits, + or #. */
    private static Pattern toPattern(String alias) {
        StringBuilder sb = new StringBuilder("(?<![\\p{L}\\p{N}+#])");
        String[] words = alias.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                sb.append("\\s+");
            }
            sb.append(Pattern.quote(words[i]));
        }
        sb.append("(?![\\p{L}\\p{N}+#])");
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /** Returns canonical skill names found in the text, in catalog order, no duplicates. */
    public Set<String> extract(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return found;
        }
        for (Skill skill : skills) {
            for (Pattern p : skill.patterns()) {
                if (p.matcher(text).find()) {
                    found.add(skill.name());
                    break;
                }
            }
        }
        return found;
    }

    public String categoryOf(String skillName) {
        return categoryByName.getOrDefault(skillName, "Other");
    }

    public int size() {
        return skills.size();
    }
}
