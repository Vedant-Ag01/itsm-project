package edu.bits.itsm.classification;

import edu.bits.itsm.domain.IncidentCategory;

import java.util.*;

public class KeywordCategoryClassifier implements CategoryClassifier {

    private final Map<IncidentCategory, List<String>> rules;

    public KeywordCategoryClassifier(Map<IncidentCategory, List<String>> rules) {
        if (rules == null) {
            throw new IllegalArgumentException("Classification rules cannot be null");
        }
        Map<IncidentCategory, List<String>> copy = new EnumMap<>(IncidentCategory.class);
        rules.forEach((cat, words) -> copy.put(cat, List.copyOf(words)));
        this.rules = Collections.unmodifiableMap(copy);
    }

    public static Map<IncidentCategory, List<String>> defaultRules() {
        Map<IncidentCategory, List<String>> map = new EnumMap<>(IncidentCategory.class);
        map.put(IncidentCategory.HARDWARE, List.of("laptop", "monitor", "keyboard", "mouse", "screen", "battery", "charger", "printer", "scanner", "dock"));
        map.put(IncidentCategory.SOFTWARE, List.of("install", "update", "os", "windows", "macos", "linux", "driver", "license", "patch", "software"));
        map.put(IncidentCategory.NETWORK, List.of("wifi", "wi-fi", "vpn", "internet", "network", "dns", "lan", "router", "latency", "offline"));
        map.put(IncidentCategory.DATABASE, List.of("database", "db", "sql", "postgres", "query", "timeout", "deadlock", "schema", "index"));
        map.put(IncidentCategory.CLOUD, List.of("aws", "azure", "gcp", "cloud", "ec2", "s3", "kubernetes", "docker", "vm", "container"));
        map.put(IncidentCategory.SECURITY, List.of("phishing", "malware", "virus", "breach", "suspicious", "unauthorized", "hack", "ransomware", "vulnerability"));
        map.put(IncidentCategory.AUTHENTICATION, List.of("login", "password", "account", "locked", "permission", "access", "mfa", "otp", "sso", "credentials"));
        map.put(IncidentCategory.APPLICATION, List.of("app", "application", "crash", "bug", "freeze", "portal", "email", "outlook", "erp", "dashboard"));
        map.put(IncidentCategory.STORAGE, List.of("storage", "disk", "drive", "quota", "space", "backup", "file", "nas", "volume", "archive"));
        return map;
    }

    @Override
    public Optional<CategorySuggestion> suggest(String title, String description) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : ""))
                .toLowerCase()
                .trim();

        if (combined.isEmpty()) {
            return Optional.empty();
        }

        IncidentCategory bestCategory = null;
        List<String> bestMatches = List.of();

        for (Map.Entry<IncidentCategory, List<String>> entry : rules.entrySet()) {
            List<String> matched = new ArrayList<>();
            for (String keyword : entry.getValue()) {
                if (containsWord(combined, keyword.toLowerCase())) {
                    matched.add(keyword);
                }
            }

            if (matched.size() > bestMatches.size()) {
                bestCategory = entry.getKey();
                bestMatches = matched;
            }
        }

        if (bestCategory == null || bestMatches.isEmpty()) {
            return Optional.empty();
        }

        double confidence = Math.min(1.0, 0.4 + (0.2 * bestMatches.size()));
        return Optional.of(new CategorySuggestion(bestCategory, confidence, List.copyOf(bestMatches)));
    }

    // Whole-word match (a plural 's' or 'es' is fine), so "os" does not match inside "hostel"
    private static boolean containsWord(String text, String keyword) {
        String pattern = "(?<![a-z0-9])" + java.util.regex.Pattern.quote(keyword) + "(?:e?s)?(?![a-z0-9])";
        return java.util.regex.Pattern.compile(pattern).matcher(text).find();
    }
}
