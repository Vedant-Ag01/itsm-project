package edu.bits.itsm.duplicate;
import edu.bits.itsm.domain.IncidentStatus;

import edu.bits.itsm.domain.Incident;

import java.util.*;
import java.util.stream.Collectors;

public class SimilarityDuplicateDetector implements DuplicateDetector {

    private static final double SIMILARITY_THRESHOLD = 0.35; // Score threshold for flag

    // Common stop words to ignore
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "my", "to", "in", "on", "of", "and", "for", "with", "not", "unable"
    );

    // Map common synonyms to a normalized token
    private static final Map<String, String> SYNONYM_MAP = Map.of(
            "down", "failing",
            "offline", "failing",
            "unavailable", "failing",
            "error", "failing",
            "broken", "failing",
            "cannot", "unable", "cant", "unable"
    );

    @Override
    public List<DuplicateMatch> findSimilar(Incident incoming, List<Incident> existing) {
        if (incoming == null || existing == null || existing.isEmpty()) {
            return List.of();
        }

        Set<String> incomingTokens = extractTokens(incoming.getTitle() + " " + incoming.getDescription());
        List<DuplicateMatch> matches = new ArrayList<>();

        for (Incident candidate : existing) {
            // Ignore closed incidents or comparing incident against itself
            if (candidate.getStatus() == IncidentStatus.CLOSED || candidate.getId().equals(incoming.getId())) {
                continue;
            }

            Set<String> candidateTokens = extractTokens(candidate.getTitle() + " " + candidate.getDescription());
            double textScore = calculateJaccardSimilarity(incomingTokens, candidateTokens);

            // Bonus score if category or service match
            double categoryBonus = 0.0;
            if (incoming.getCategory() != null && incoming.getCategory().equals(candidate.getCategory())) {
                categoryBonus += 0.15;
            }

            double totalScore = Math.min(1.0, textScore + categoryBonus);

            if (totalScore >= SIMILARITY_THRESHOLD) {
                matches.add(new DuplicateMatch(candidate, totalScore));
            }
        }

        // Sort descending by score
        matches.sort((m1, m2) -> Double.compare(m2.score(), m1.score()));
        return matches;
    }

    private Set<String> extractTokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }

        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] words = cleaned.split("\\s+");

        return Arrays.stream(words)
                .filter(w -> !w.isBlank() && !STOP_WORDS.contains(w))
                .map(w -> SYNONYM_MAP.getOrDefault(w, w))
                .collect(Collectors.toSet());
    }

    private double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }
}