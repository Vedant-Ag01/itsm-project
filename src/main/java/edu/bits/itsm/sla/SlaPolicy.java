package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Priority;

import java.time.Duration;

public interface SlaPolicy {
    Duration responseTarget(Priority priority);

    Duration resolutionTarget(Priority priority);
}