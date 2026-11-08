package edu.bits.itsm.escalation;

import edu.bits.itsm.domain.Priority;

import java.time.Duration;

public interface EscalationPolicy {
    // Returns the same level when no escalation is needed yet.
    EscalationLevel nextLevel(Priority priority, EscalationLevel current,
                              Duration timeUnacknowledged);
}