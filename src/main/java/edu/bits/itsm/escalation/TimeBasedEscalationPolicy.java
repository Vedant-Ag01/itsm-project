package edu.bits.itsm.escalation;

import edu.bits.itsm.domain.Priority;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;


public class TimeBasedEscalationPolicy implements EscalationPolicy {

    
    public static final Map<Priority, Duration> DEFAULT_INTERVALS = Map.of(
            Priority.P1, Duration.ofMinutes(10),
            Priority.P2, Duration.ofMinutes(30),
            Priority.P3, Duration.ofHours(2));

    private static final EscalationLevel[] LEVELS = EscalationLevel.values();
        
    private final Map<Priority, Duration> intervals;

    public TimeBasedEscalationPolicy() {
        this(DEFAULT_INTERVALS);
    }


    public TimeBasedEscalationPolicy(Map<Priority, Duration> intervals) {
        if (intervals == null) {
            throw new IllegalArgumentException("intervals must not be null");
        }
        Map<Priority, Duration> copy = new EnumMap<>(Priority.class);
        for (Map.Entry<Priority, Duration> entry : intervals.entrySet()) {
            Priority priority = entry.getKey();
            Duration interval = entry.getValue();
            if (priority == null) {
                throw new IllegalArgumentException("interval map must not contain a null priority");
            }
            if (priority == Priority.P4) {
                throw new IllegalArgumentException("P4 must not have an interval: it never escalates automatically");
            }
            if (interval == null) {
                throw new IllegalArgumentException("interval for " + priority + " must not be null");
            }
            if (interval.isZero() || interval.isNegative()) {
                throw new IllegalArgumentException("interval for " + priority + " must be positive, was " + interval);
            }
            try {
                
                interval.multipliedBy(LEVELS.length - 1);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("interval for " + priority + " is too large", e);
            }
            copy.put(priority, interval);
        }
        for (Priority required : new Priority[] {Priority.P1, Priority.P2, Priority.P3}) {
            if (!copy.containsKey(required)) {
                throw new IllegalArgumentException("missing interval for " + required);
            }
        }
        this.intervals = copy;
    }

    public Optional<Duration> intervalFor(Priority priority) {
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }
        return Optional.ofNullable(intervals.get(priority));
    }

    @Override
    public EscalationLevel nextLevel(Priority priority, EscalationLevel current,
                                     Duration timeUnacknowledged) {
      
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }
        if (current == null) {
            throw new IllegalArgumentException("current level must not be null");
        }
        if (timeUnacknowledged == null) {
            throw new IllegalArgumentException("timeUnacknowledged must not be null");
        }
        if (timeUnacknowledged.isNegative()) {
            throw new IllegalArgumentException("timeUnacknowledged must not be negative, was " + timeUnacknowledged);
        }

        Duration interval = intervals.get(priority);
        int rank = current.ordinal();
        if (interval == null || rank == LEVELS.length - 1) {
            return current; 
        }
        
        Duration threshold = interval.multipliedBy(rank + 1L);
        if (timeUnacknowledged.compareTo(threshold) >= 0) {
            return LEVELS[rank + 1];
        }
        return current;
    }
}
