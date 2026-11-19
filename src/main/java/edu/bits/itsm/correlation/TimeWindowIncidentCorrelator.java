package edu.bits.itsm.correlation;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TimeWindowIncidentCorrelator implements IncidentCorrelator {

    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(60);
    public static final int DEFAULT_MIN_GROUP_SIZE = 2;

    private final Duration window;
    private final int minGroupSize;

    public TimeWindowIncidentCorrelator() {
        this(DEFAULT_WINDOW, DEFAULT_MIN_GROUP_SIZE);
    }

    public TimeWindowIncidentCorrelator(Duration window, int minGroupSize) {
        if (window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Correlation window must be positive");
        }
        if (minGroupSize < 2) {
            throw new IllegalArgumentException("Minimum group size must be at least 2");
        }
        this.window = window;
        this.minGroupSize = minGroupSize;
    }

    @Override
    public List<CorrelationGroup> findGroups(List<Incident> incidents, LocalDateTime now) {
        if (incidents == null || incidents.isEmpty()) {
            return List.of();
        }

        LocalDateTime referenceTime = (now != null) ? now : LocalDateTime.now();
        LocalDateTime cutoff = referenceTime.minus(window);

        // Filter to active incidents created within [cutoff, referenceTime]
        List<Incident> recentActive = incidents.stream()
                .filter(Objects::nonNull)
                .filter(this::isActive)
                .filter(i -> i.getCreatedAt() != null
                        && !i.getCreatedAt().isBefore(cutoff)
                        && !i.getCreatedAt().isAfter(referenceTime))
                .sorted(Comparator.comparing(Incident::getCreatedAt))
                .toList();

        // Group by affected service name (case-insensitive)
        Map<String, List<Incident>> byService = new LinkedHashMap<>();
        for (Incident incident : recentActive) {
            String serviceKey = incident.getService().getName().trim().toLowerCase();
            byService.computeIfAbsent(serviceKey, k -> new ArrayList<>()).add(incident);
        }

        List<CorrelationGroup> groups = new ArrayList<>();
        for (List<Incident> cluster : byService.values()) {
            if (cluster.size() < minGroupSize) {
                continue;
            }

            String serviceName = cluster.get(0).getService().getName();
            LocalDateTime first = cluster.get(0).getCreatedAt();
            LocalDateTime last = cluster.get(cluster.size() - 1).getCreatedAt();
            IncidentCategory dominantCategory = findDominantCategory(cluster);

            String summary = cluster.size() + " active incidents on " + serviceName
                    + " within " + window.toMinutes() + " minutes";

            groups.add(new CorrelationGroup(
                    serviceName,
                    dominantCategory,
                    first,
                    last,
                    cluster,
                    summary
            ));
        }

        // Largest incident spikes first
        groups.sort((g1, g2) -> Integer.compare(g2.size(), g1.size()));
        return groups;
    }

    private boolean isActive(Incident incident) {
        IncidentStatus status = incident.getStatus();
        return status != IncidentStatus.RESOLVED
                && status != IncidentStatus.VERIFIED
                && status != IncidentStatus.CLOSED;
    }

    private IncidentCategory findDominantCategory(List<Incident> cluster) {
        Map<IncidentCategory, Integer> counts = new LinkedHashMap<>();
        for (Incident incident : cluster) {
            if (incident.getCategory() != null) {
                counts.merge(incident.getCategory(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
