package edu.bits.itsm.reporting;

import edu.bits.itsm.domain.Incident;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportService {

    public IncidentOverviewReport overview(List<Incident> incidents,
                                           Function<Incident, String> slaEvaluator) {
        if (incidents == null || incidents.isEmpty()) {
            return new IncidentOverviewReport(0, 0, 0, 0, 0);
        }

        long total = incidents.size();

        long resolved = incidents.stream()
                .filter(inc -> isResolvedOrClosed(String.valueOf(inc.getStatus())))
                .count();

        long open = total - resolved;

        long criticalP1 = incidents.stream()
                .filter(inc -> "P1".equalsIgnoreCase(String.valueOf(inc.getPriority())))
                .count();

        long slaViolations = 0;
        if (slaEvaluator != null) {
            slaViolations = incidents.stream()
                    .filter(inc -> "BREACHED".equalsIgnoreCase(slaEvaluator.apply(inc)))
                    .count();
        }

        return new IncidentOverviewReport(total, open, criticalP1, resolved, slaViolations);
    }

    public List<WorkloadEntry> technicianWorkload(List<Incident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return List.of();
        }

        // Group active (non-closed/resolved) incidents by assigned technician
        Map<String, Long> countsByTech = incidents.stream()
                .filter(inc -> !isResolvedOrClosed(String.valueOf(inc.getStatus())))
                .filter(inc -> inc.getAssignedTechnician() != null)
                .collect(Collectors.groupingBy(
                        inc -> String.valueOf(inc.getAssignedTechnician()),
                        Collectors.counting()
                ));

        return countsByTech.entrySet().stream()
                .map(entry -> new WorkloadEntry(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.activeIncidentCount(), a.activeIncidentCount()))
                .toList();
    }

    public List<CategoryBreakdown> categoryBreakdown(List<Incident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return List.of();
        }

        Map<String, Long> countsByCategory = incidents.stream()
                .collect(Collectors.groupingBy(
                        inc -> inc.getCategory() != null ? String.valueOf(inc.getCategory()) : "UNCATEGORIZED",
                        Collectors.counting()
                ));

        return countsByCategory.entrySet().stream()
                .map(entry -> new CategoryBreakdown(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .toList();
    }

    public double averageResolutionMinutes(List<Incident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return 0.0;
        }

        return incidents.stream()
                .filter(inc -> inc.getCreatedAt() != null && inc.getResolvedAt() != null)
                .mapToLong(inc -> Duration.between(inc.getCreatedAt(), inc.getResolvedAt()).toMinutes())
                .average()
                .orElse(0.0);
    }

    private boolean isResolvedOrClosed(String status) {
        return "RESOLVED".equalsIgnoreCase(status)
                || "VERIFIED".equalsIgnoreCase(status)
                || "CLOSED".equalsIgnoreCase(status);
    }
}