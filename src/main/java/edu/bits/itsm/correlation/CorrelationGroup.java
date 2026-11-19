package edu.bits.itsm.correlation;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;

import java.time.LocalDateTime;
import java.util.List;

public record CorrelationGroup(
        String serviceName,
        IncidentCategory category,
        LocalDateTime windowStart,
        LocalDateTime windowEnd,
        List<Incident> incidents,
        String summary
) {
    public CorrelationGroup {
        incidents = incidents == null ? List.of() : List.copyOf(incidents);
    }

    public int size() {
        return incidents.size();
    }
}
