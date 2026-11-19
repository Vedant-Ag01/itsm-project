package edu.bits.itsm.suggestion;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SimilarityResolutionSuggester implements ResolutionSuggester {

    private static final double MIN_SIMILARITY_THRESHOLD = 0.25;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "my", "to", "in", "on", "of", "and", "for", "with", "not", "unable"
    );

    private static final Map<String, String> SYNONYM_MAP = Map.of(
            "down", "failing",
            "offline", "failing",
            "unavailable", "failing",
            "error", "failing",
            "broken", "failing",
            "cannot", "unable",
            "cant", "unable"
    );

    @Override
    public List<ResolutionSuggestion> suggest(Incident incoming, List<Incident> history, int limit) {
        if (incoming == null || history == null || history.isEmpty() || limit <= 0) {
            return List.of();
        }

        Set<String> incomingTokens = extractTokens(incoming.getTitle() + " " + incoming.getDescription());
        List<ResolutionSuggestion> suggestions = new ArrayList<>();

        for (Incident past : history) {
            if (past == null || past.getId().equals(incoming.getId())) {
                continue;
            }
            if (!isResolvedWithNote(past)) {
                continue;
            }

            Set<String> pastTokens = extractTokens(past.getTitle() + " " + past.getDescription());
            double score = calculateJaccardSimilarity(incomingTokens, pastTokens);

            if (incoming.getService() != null && past.getService() != null
                    && incoming.getService().getName().equalsIgnoreCase(past.getService().getName())) {
                score += 0.15;
            }
            if (incoming.getCategory() != null && incoming.getCategory() == past.getCategory()) {
                score += 0.10;
            }

            double finalScore = Math.min(1.0, score);
            if (finalScore >= MIN_SIMILARITY_THRESHOLD) {
                suggestions.add(new ResolutionSuggestion(
                        past.getId(),
                        past.getTitle(),
                        past.getResolutionNote(),
                        finalScore
                ));
            }
        }

        suggestions.sort((s1, s2) -> Double.compare(s2.similarity(), s1.similarity()));
        return suggestions.stream().limit(limit).toList();
    }

    private boolean isResolvedWithNote(Incident incident) {
        IncidentStatus status = incident.getStatus();
        boolean done = status == IncidentStatus.RESOLVED
                || status == IncidentStatus.VERIFIED
                || status == IncidentStatus.CLOSED;
        return done && incident.getResolutionNote() != null && !incident.getResolutionNote().isBlank();
    }

    private Set<String> extractTokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(w -> !w.isBlank() && !STOP_WORDS.contains(w))
                .map(w -> SYNONYM_MAP.getOrDefault(w, w))
                .collect(Collectors.toSet());
    }

    private double calculateJaccardSimilarity(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);

        Set<String> union = new HashSet<>(a);
        union.addAll(b);

        return (double) intersection.size() / union.size();
    }
}
