package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Priority;

import java.time.LocalDateTime;

public interface SlaCalculator {
    LocalDateTime responseDeadline(LocalDateTime createdAt, Priority priority);

    LocalDateTime resolutionDeadline(LocalDateTime createdAt, Priority priority);

    // acknowledgedAt and resolvedAt are null until they happen.
    SlaStatus evaluate(LocalDateTime createdAt, Priority priority,
                       LocalDateTime acknowledgedAt, LocalDateTime resolvedAt,
                       LocalDateTime now);
}