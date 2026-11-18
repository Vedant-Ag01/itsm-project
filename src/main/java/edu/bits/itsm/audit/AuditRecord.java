package edu.bits.itsm.audit;

import java.time.LocalDateTime;
import edu.bits.itsm.event.IncidentEventType;

public record AuditRecord(
        String incidentId,
        IncidentEventType eventType,
        String actor,
        String previousStatus,
        String newStatus,
        String description,
        LocalDateTime timestamp
) {
    public String getFormattedLog() {
        return String.format("[%s] Incident %s - %s by %s (Status: %s -> %s): %s",
                timestamp, incidentId, eventType, actor, 
                previousStatus != null ? previousStatus : "NONE", 
                newStatus, description);
    }
}