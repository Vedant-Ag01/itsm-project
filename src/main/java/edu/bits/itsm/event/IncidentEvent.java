package edu.bits.itsm.event;

import edu.bits.itsm.domain.IncidentStatus;

import java.time.LocalDateTime;

public record IncidentEvent(String incidentId,
                            IncidentEventType type,
                            String actor,
                            IncidentStatus previousStatus,
                            IncidentStatus newStatus,
                            String description,
                            LocalDateTime timestamp) {
}