    package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Priority;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;


public class DefaultSlaCalculator implements SlaCalculator {

    private final SlaPolicy policy;

    public DefaultSlaCalculator(SlaPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        this.policy = policy;
    }

    @Override
    public LocalDateTime responseDeadline(LocalDateTime createdAt, Priority priority) {
        requireCreatedAtAndPriority(createdAt, priority);
        Targets targets = targetsFor(priority);
        return plusTarget(createdAt, targets.response());
    }

    @Override
    public LocalDateTime resolutionDeadline(LocalDateTime createdAt, Priority priority) {
        requireCreatedAtAndPriority(createdAt, priority);
        Targets targets = targetsFor(priority);
        return plusTarget(createdAt, targets.resolution());
    }

    @Override
    public SlaStatus evaluate(LocalDateTime createdAt, Priority priority,
                              LocalDateTime acknowledgedAt, LocalDateTime resolvedAt,
                              LocalDateTime now) {
        requireCreatedAtAndPriority(createdAt, priority);
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        checkTimestampOrder(createdAt, acknowledgedAt, resolvedAt, now);
        Targets targets = targetsFor(priority);
    
        LocalDateTime responseDue = plusTarget(createdAt, targets.response());
        LocalDateTime resolutionDue = plusTarget(createdAt, targets.resolution());

        SlaStatus response = judge(createdAt, targets.response(), responseDue,
                acknowledgedAt, now);
        SlaStatus resolution = judge(createdAt, targets.resolution(), resolutionDue,
                resolvedAt, now);

      
        if (response == SlaStatus.BREACHED || resolution == SlaStatus.BREACHED) {
            return SlaStatus.BREACHED;
        }
        if (response == SlaStatus.AT_RISK || resolution == SlaStatus.AT_RISK) {
            return SlaStatus.AT_RISK;
        }
        return SlaStatus.ON_TRACK;
    }


    static Duration warningOffset(Duration target) {
        return target.multipliedBy(4).dividedBy(5);
    }

 
    private static SlaStatus judge(LocalDateTime createdAt, Duration target,
                                   LocalDateTime deadline, LocalDateTime completedAt,
                                   LocalDateTime now) {
        if (completedAt != null) {
            return completedAt.isAfter(deadline) ? SlaStatus.BREACHED : SlaStatus.ON_TRACK;
        }
        if (now.isAfter(deadline)) {
            return SlaStatus.BREACHED;
        }

        LocalDateTime warningPoint = plusTarget(createdAt, warningOffset(target));
        if (now.isBefore(warningPoint)) {
            return SlaStatus.ON_TRACK;
        }
        return SlaStatus.AT_RISK;
    }

    private static void requireCreatedAtAndPriority(LocalDateTime createdAt, Priority priority) {
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }
    }

    private static void checkTimestampOrder(LocalDateTime createdAt, LocalDateTime acknowledgedAt,
                                            LocalDateTime resolvedAt, LocalDateTime now) {
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("now (" + now
                    + ") must not be before createdAt (" + createdAt + ")");
        }
        if (acknowledgedAt != null && acknowledgedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("acknowledgedAt (" + acknowledgedAt
                    + ") must not be before createdAt (" + createdAt + ")");
        }
        if (resolvedAt != null && resolvedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("resolvedAt (" + resolvedAt
                    + ") must not be before createdAt (" + createdAt + ")");
        }
        if (acknowledgedAt != null && acknowledgedAt.isAfter(now)) {
            throw new IllegalArgumentException("acknowledgedAt (" + acknowledgedAt
                    + ") must not be after now (" + now + ")");
        }
        if (resolvedAt != null && resolvedAt.isAfter(now)) {
            throw new IllegalArgumentException("resolvedAt (" + resolvedAt
                    + ") must not be after now (" + now + ")");
        }
        if (resolvedAt != null && acknowledgedAt == null) {
            throw new IllegalArgumentException("resolvedAt is set but acknowledgedAt is missing");
        }
        if (resolvedAt != null && resolvedAt.isBefore(acknowledgedAt)) {
            throw new IllegalArgumentException("resolvedAt (" + resolvedAt
                    + ") must not be before acknowledgedAt (" + acknowledgedAt + ")");
        }
    }


    private Targets targetsFor(Priority priority) {
        Duration response = policy.responseTarget(priority);
        Duration resolution = policy.resolutionTarget(priority);
        if (response == null || resolution == null) {
            throw new IllegalStateException("missing SLA configuration for " + priority);
        }
        if (response.isZero() || response.isNegative()
                || resolution.isZero() || resolution.isNegative()) {
            throw new IllegalStateException("SLA targets for " + priority
                    + " must be positive, but were " + response + " and " + resolution);
        }
        if (response.compareTo(resolution) > 0) {
            throw new IllegalStateException("response target for " + priority + " ("
                    + response + ") is longer than its resolution target (" + resolution + ")");
        }
        return new Targets(response, resolution);
    }


    private static LocalDateTime plusTarget(LocalDateTime start, Duration amount) {
        try {
            return start.plus(amount);
        } catch (DateTimeException | ArithmeticException e) {
            throw new IllegalArgumentException("SLA time " + start + " + " + amount
                    + " is outside the supported date range", e);
        }
    }

    private record Targets(Duration response, Duration resolution) {
    }
}
