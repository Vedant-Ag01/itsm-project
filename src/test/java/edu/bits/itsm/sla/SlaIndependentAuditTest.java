package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Priority;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Independent audit: elapsed-time inequalities, without production deadline/warning helpers. */
class SlaIndependentAuditTest {
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 12, 31, 23, 58);
    private static final long SECOND = 1_000_000_000L;
    private static final long[] RESPONSE = {300 * SECOND, 900 * SECOND, 3600 * SECOND, 14400 * SECOND};
    private static final long[] RESOLUTION = {1800 * SECOND, 7200 * SECOND, 28800 * SECOND, 86400 * SECOND};

    @Test
    void allDefaultPrioritiesMatchIndependentOracleAcrossCompletionAndBoundaryCombinations() {
        SlaCalculator calculator = new DefaultSlaCalculator(new DefaultSlaPolicy());
        int checked = 0;
        for (Priority priority : Priority.values()) {
            long response = RESPONSE[priority.ordinal()];
            long resolution = RESOLUTION[priority.ordinal()];
            long[] points = boundaryPoints(response, resolution);
            for (long now : points) {
                // -1 is the test-only marker for an unfinished obligation.
                long[] completions = new long[points.length + 1];
                completions[0] = -1;
                System.arraycopy(points, 0, completions, 1, points.length);
                for (long ack : completions) {
                    if (ack > now) continue;
                    for (long resolved : completions) {
                        if (resolved > now || (resolved >= 0 && (ack < 0 || resolved < ack))) continue;
                        check(calculator, priority, response, resolution, ack, resolved, now);
                        checked++;
                    }
                }
            }
        }
        System.out.println("Independent default SLA oracle: " + checked + " valid combinations passed");
    }

    @Test
    void seededCustomTargetsMatchIndependentOracleForBothClocks() {
        Random random = new Random(20261114L);
        int checked = 0;
        for (int sample = 0; sample < 1000; sample++) {
            // Multiples of 5 ns have exact 80% thresholds: no unresolved rounding policy.
            long response = (1 + random.nextLong(6_000_000_000L)) * 5;
            long resolution = response + random.nextLong(30_000_000_000L) * 5;
            SlaPolicy policy = new SlaPolicy() {
                public Duration responseTarget(Priority priority) { return Duration.ofNanos(response); }
                public Duration resolutionTarget(Priority priority) { return Duration.ofNanos(resolution); }
            };
            SlaCalculator calculator = new DefaultSlaCalculator(policy);
            for (long now : boundaryPoints(response, resolution)) {
                // Unanswered, answered on time, and fully completed histories.
                check(calculator, Priority.P1, response, resolution, -1, -1, now);
                check(calculator, Priority.P1, response, resolution, 0, -1, now);
                long ack = random.nextLong(now + 1);
                long resolved = ack + random.nextLong(now - ack + 1);
                check(calculator, Priority.P1, response, resolution, ack, resolved, now);
                checked += 3;
            }
        }
        System.out.println("Independent custom SLA oracle: " + checked + " valid combinations passed");
    }

    private static long[] boundaryPoints(long response, long resolution) {
        long rw = response / 5 * 4;
        long sw = resolution / 5 * 4;
        return Arrays.stream(new long[]{0, rw - 1, rw, rw + 1, response - 1, response, response + 1,
                sw - 1, sw, sw + 1, resolution - 1, resolution, resolution + 1})
                .filter(value -> value >= 0).distinct().sorted().toArray();
    }

    private static void check(SlaCalculator calculator, Priority priority, long response, long resolution,
                              long ack, long resolved, long now) {
        int severity = Math.max(obligation(response, ack, now), obligation(resolution, resolved, now));
        SlaStatus expected = switch (severity) {
            case 0 -> SlaStatus.ON_TRACK;
            case 1 -> SlaStatus.AT_RISK;
            default -> SlaStatus.BREACHED;
        };
        assertEquals(expected, calculator.evaluate(CREATED, priority,
                ack < 0 ? null : CREATED.plusNanos(ack),
                resolved < 0 ? null : CREATED.plusNanos(resolved), CREATED.plusNanos(now)),
                () -> "priority=" + priority + ", targets=" + response + "/" + resolution
                        + ", ack=" + ack + ", resolved=" + resolved + ", now=" + now);
    }

    private static int obligation(long target, long completion, long now) {
        if (completion >= 0) return completion > target ? 2 : 0;
        if (now > target) return 2;
        // Cross multiplication independently checks elapsed/target >= 4/5.
        return BigInteger.valueOf(now).multiply(BigInteger.valueOf(5))
                .compareTo(BigInteger.valueOf(target).multiply(BigInteger.valueOf(4))) >= 0 ? 1 : 0;
    }
}
