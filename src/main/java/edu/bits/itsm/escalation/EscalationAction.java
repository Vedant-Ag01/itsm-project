package edu.bits.itsm.escalation;

import edu.bits.itsm.domain.Priority;

import java.time.Duration;
import java.time.LocalDateTime;

public record EscalationAction(String incidentId, Priority priority,
                               EscalationLevel previousLevel, EscalationLevel newLevel,
                               Duration timeUnacknowledged, LocalDateTime checkedAt,
                               String reason) {

    public EscalationAction {
        if (incidentId == null || priority == null || previousLevel == null || newLevel == null
                || timeUnacknowledged == null || checkedAt == null || reason == null) {
            throw new IllegalArgumentException("escalation action values must not be null");
        }
        if (newLevel.ordinal() != previousLevel.ordinal() + 1) {
            throw new IllegalArgumentException(
                    "escalation must move exactly one level up, was " + previousLevel + " -> " + newLevel);
        }
    }
}
