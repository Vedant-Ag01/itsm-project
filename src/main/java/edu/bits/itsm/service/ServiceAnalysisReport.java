package edu.bits.itsm.service;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.Priority;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// "Which services cause us the most trouble?" One row per service, built from the incidents.
public class ServiceAnalysisReport {

    public record Entry(
            String serviceName,
            String criticality,
            int totalIncidents,
            int openIncidents,
            int resolvedIncidents,
            int p1Incidents,
            int slaBreaches,
            Double averageResolutionMinutes,
            String mostCommonCategory) { }

    // slaStatusOf says "ON_TRACK", "AT_RISK", "BREACHED" (or null) for an incident
    public static List<Entry> build(List<Incident> incidents, Function<Incident, String> slaStatusOf) {
        Map<String, List<Incident>> byService = new LinkedHashMap<>();
        for (Incident incident : incidents) {
            byService.computeIfAbsent(incident.getService().getName(), k -> new ArrayList<>())
                    .add(incident);
        }

        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, List<Incident>> group : byService.entrySet()) {
            entries.add(entryFor(group.getKey(), group.getValue(), slaStatusOf));
        }
        entries.sort((a, b) -> {
            int byCount = Integer.compare(b.totalIncidents(), a.totalIncidents());
            return byCount != 0 ? byCount : a.serviceName().compareTo(b.serviceName());
        });
        return entries;
    }

    private static Entry entryFor(String name, List<Incident> incidents,
                                  Function<Incident, String> slaStatusOf) {
        int open = 0;
        int resolved = 0;
        int p1 = 0;
        int breaches = 0;
        long resolvedCount = 0;
        long totalMinutes = 0;
        Map<IncidentCategory, Integer> categories = new EnumMap<>(IncidentCategory.class);

        for (Incident incident : incidents) {
            IncidentStatus status = incident.getStatus();
            if (status == IncidentStatus.RESOLVED || status == IncidentStatus.VERIFIED
                    || status == IncidentStatus.CLOSED) {
                resolved++;
            } else {
                open++;
            }
            if (incident.getPriority() == Priority.P1) {
                p1++;
            }
            if ("BREACHED".equals(slaStatusOf.apply(incident))) {
                breaches++;
            }
            if (incident.getResolvedAt() != null) {
                resolvedCount++;
                totalMinutes += Duration.between(incident.getCreatedAt(),
                        incident.getResolvedAt()).toMinutes();
            }
            if (incident.getCategory() != null) {
                categories.merge(incident.getCategory(), 1, Integer::sum);
            }
        }

        Double average = null;
        if (resolvedCount > 0) {
            average = Math.round(totalMinutes * 10.0 / resolvedCount) / 10.0;
        }

        String topCategory = null;
        int best = 0;
        for (Map.Entry<IncidentCategory, Integer> e : categories.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                topCategory = e.getKey().name();
            }
        }

        return new Entry(name, incidents.get(0).getService().getCriticality().name(),
                incidents.size(), open, resolved, p1, breaches, average, topCategory);
    }
}
