package edu.bits.itsm.audit;

import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.event.IncidentEventType;

import java.time.LocalDateTime;

public record AuditRecord(
        String incidentId,
        IncidentEventType eventType,
        String actor,
        IncidentStatus previousStatus,
        IncidentStatus newStatus,
        String description,
        LocalDateTime timestamp
) {
    public String getFormattedLog() {
        return String.format("[%s] Incident %s - %s by %s (Status: %s -> %s): %s",
                timestamp, incidentId, eventType, actor,
                previousStatus != null ? previousStatus.name() : "NONE",
                newStatus, description);
    }
}