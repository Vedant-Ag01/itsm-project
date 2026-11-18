package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Priority;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

public class DefaultSlaPolicy implements SlaPolicy {

    private final Map<Priority, Duration> responseTargets;
    private final Map<Priority, Duration> resolutionTargets;


    public DefaultSlaPolicy() {
        this(Map.of(
                        Priority.P1, Duration.ofMinutes(5),
                        Priority.P2, Duration.ofMinutes(15),
                        Priority.P3, Duration.ofHours(1),
                        Priority.P4, Duration.ofHours(4)),
                Map.of(
                        Priority.P1, Duration.ofMinutes(30),
                        Priority.P2, Duration.ofHours(2),
                        Priority.P3, Duration.ofHours(8),
                        Priority.P4, Duration.ofHours(24)));
    }


    public DefaultSlaPolicy(Map<Priority, Duration> responseTargets,
                            Map<Priority, Duration> resolutionTargets) {
        this.responseTargets = copyTargets(responseTargets, "response");
        this.resolutionTargets = copyTargets(resolutionTargets, "resolution");
        for (Priority priority : Priority.values()) {
            Duration response = this.responseTargets.get(priority);
            Duration resolution = this.resolutionTargets.get(priority);

            if (response.compareTo(resolution) > 0) {
                throw new IllegalArgumentException("response target for " + priority
                        + " (" + response + ") is longer than its resolution target ("
                        + resolution + ")");
            }
        }
    }


    private static Map<Priority, Duration> copyTargets(Map<Priority, Duration> targets,
                                                       String kind) {
        if (targets == null) {
            throw new IllegalArgumentException(kind + " targets must not be null");
        }
        Map<Priority, Duration> copy = new EnumMap<>(Priority.class);
        for (Map.Entry<Priority, Duration> entry : targets.entrySet()) {
            Priority priority = entry.getKey();
            Duration target = entry.getValue();
            if (priority == null) {
                throw new IllegalArgumentException(kind + " targets contain a null priority");
            }
            if (target == null) {
                throw new IllegalArgumentException(kind + " target for " + priority
                        + " must not be null");
            }
            if (target.isZero() || target.isNegative()) {
                throw new IllegalArgumentException(kind + " target for " + priority
                        + " must be positive, but was " + target);
            }
            copy.put(priority, target);
        }
        for (Priority priority : Priority.values()) {
            if (!copy.containsKey(priority)) {
                throw new IllegalArgumentException(kind + " target for " + priority
                        + " is missing");
            }
        }
        return copy;
    }


    @Override
    public Duration responseTarget(Priority priority) {
        requirePriority(priority);
        return responseTargets.get(priority);
    }

    @Override
    public Duration resolutionTarget(Priority priority) {
        requirePriority(priority);
        return resolutionTargets.get(priority);
    }

    private static void requirePriority(Priority priority) {
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }
    }
}
