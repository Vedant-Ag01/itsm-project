package edu.bits.itsm.escalation;

import edu.bits.itsm.domain.Priority;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static edu.bits.itsm.escalation.EscalationLevel.IT_MANAGER;
import static edu.bits.itsm.escalation.EscalationLevel.SYSTEM_ADMIN;
import static edu.bits.itsm.escalation.EscalationLevel.TEAM_LEAD;
import static edu.bits.itsm.escalation.EscalationLevel.TECHNICIAN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Expected levels are worked out by hand from the agreed rules:
// thresholds counted from creation, step out of rank r due at (r + 1) intervals,
// due "at or after" (>=), one step per call, IT_MANAGER is the top, P4 never.
// P1: 10 / 20 / 30 min.  P2: 30 / 60 / 90 min.  P3: 2 / 4 / 6 h.
class TimeBasedEscalationPolicyTest {

    private static final Duration NS = Duration.ofNanos(1);

    // Used through the shared interface, to show the policy is interchangeable.
    private final EscalationPolicy policy = new TimeBasedEscalationPolicy();

    private static Duration min(long minutes) {
        return Duration.ofMinutes(minutes);
    }

    private static Duration hours(long hours) {
        return Duration.ofHours(hours);
    }

    private void expect(EscalationLevel expected, Priority priority, EscalationLevel current,
                        Duration elapsed) {
        assertEquals(expected, policy.nextLevel(priority, current, elapsed),
                priority + " at " + current + " after " + elapsed);
    }

    // ---------- shared contract ----------

    @Test
    void levelOrderMatchesTheHierarchyThePolicyReliesOn() {
        assertArrayEquals(new EscalationLevel[] {TECHNICIAN, TEAM_LEAD, SYSTEM_ADMIN, IT_MANAGER},
                EscalationLevel.values());
    }

    // ---------- P1 (10 min) ----------

    @Test
    void p1NoEscalationYet() {
        expect(TECHNICIAN, Priority.P1, TECHNICIAN, Duration.ZERO);
        expect(TECHNICIAN, Priority.P1, TECHNICIAN, min(9).plusSeconds(59));
        expect(TECHNICIAN, Priority.P1, TECHNICIAN, min(10).minus(NS));
    }

    @Test
    void p1FirstEscalationAtExactlyTenMinutes() {
        expect(TEAM_LEAD, Priority.P1, TECHNICIAN, min(10));
        expect(TEAM_LEAD, Priority.P1, TECHNICIAN, min(10).plus(NS));
    }

    @Test
    void p1SecondEscalationNeedsTwentyMinutesFromCreation() {
        expect(TEAM_LEAD, Priority.P1, TEAM_LEAD, min(10));
        expect(TEAM_LEAD, Priority.P1, TEAM_LEAD, min(19).plusSeconds(59));
        expect(TEAM_LEAD, Priority.P1, TEAM_LEAD, min(20).minus(NS));
        expect(SYSTEM_ADMIN, Priority.P1, TEAM_LEAD, min(20));
        expect(SYSTEM_ADMIN, Priority.P1, TEAM_LEAD, min(20).plus(NS));
    }

    @Test
    void p1ReachesTheTopAtThirtyMinutes() {
        expect(SYSTEM_ADMIN, Priority.P1, SYSTEM_ADMIN, min(30).minus(NS));
        expect(IT_MANAGER, Priority.P1, SYSTEM_ADMIN, min(30));
        expect(IT_MANAGER, Priority.P1, SYSTEM_ADMIN, min(30).plus(NS));
    }

    @Test
    void missedChecksStillMoveOnlyOneStep() {
        expect(TEAM_LEAD, Priority.P1, TECHNICIAN, min(35));
        expect(TEAM_LEAD, Priority.P1, TECHNICIAN, hours(24));
        expect(SYSTEM_ADMIN, Priority.P1, TEAM_LEAD, hours(24));
    }

    @Test
    void topLevelNeverGoesHigher() {
        expect(IT_MANAGER, Priority.P1, IT_MANAGER, Duration.ZERO);
        expect(IT_MANAGER, Priority.P1, IT_MANAGER, min(30));
        expect(IT_MANAGER, Priority.P1, IT_MANAGER, Duration.ofSeconds(Long.MAX_VALUE, 999_999_999));
        expect(IT_MANAGER, Priority.P3, IT_MANAGER, hours(1000));
    }

    // ---------- P2 (30 min) ----------

    @Test
    void p2UsesThirtyMinuteSteps() {
        expect(TECHNICIAN, Priority.P2, TECHNICIAN, min(30).minus(NS));
        expect(TEAM_LEAD, Priority.P2, TECHNICIAN, min(30));
        expect(TEAM_LEAD, Priority.P2, TECHNICIAN, min(30).plus(NS));

        expect(TEAM_LEAD, Priority.P2, TEAM_LEAD, min(60).minus(NS));
        expect(SYSTEM_ADMIN, Priority.P2, TEAM_LEAD, min(60));
        expect(SYSTEM_ADMIN, Priority.P2, TEAM_LEAD, min(60).plus(NS));

        expect(SYSTEM_ADMIN, Priority.P2, SYSTEM_ADMIN, min(90).minus(NS));
        expect(IT_MANAGER, Priority.P2, SYSTEM_ADMIN, min(90));
        expect(IT_MANAGER, Priority.P2, SYSTEM_ADMIN, min(90).plus(NS));
    }

    // ---------- P3 (2 h) ----------

    @Test
    void p3UsesTwoHourSteps() {
        expect(TECHNICIAN, Priority.P3, TECHNICIAN, hours(2).minus(NS));
        expect(TEAM_LEAD, Priority.P3, TECHNICIAN, hours(2));
        expect(TEAM_LEAD, Priority.P3, TECHNICIAN, hours(2).plus(NS));

        expect(TEAM_LEAD, Priority.P3, TEAM_LEAD, hours(4).minus(NS));
        expect(SYSTEM_ADMIN, Priority.P3, TEAM_LEAD, hours(4));
        expect(SYSTEM_ADMIN, Priority.P3, TEAM_LEAD, hours(4).plus(NS));

        expect(SYSTEM_ADMIN, Priority.P3, SYSTEM_ADMIN, hours(6).minus(NS));
        expect(IT_MANAGER, Priority.P3, SYSTEM_ADMIN, hours(6));
        expect(IT_MANAGER, Priority.P3, SYSTEM_ADMIN, hours(6).plus(NS));
    }

    // ---------- P4 ----------

    @Test
    void p4NeverEscalatesFromAnyLevel() {
        Duration[] elapsedTimes = {Duration.ZERO, min(10), hours(6), hours(10_000),
                Duration.ofSeconds(Long.MAX_VALUE)};
        for (EscalationLevel level : EscalationLevel.values()) {
            for (Duration elapsed : elapsedTimes) {
                expect(level, Priority.P4, level, elapsed);
            }
        }
    }

    // ---------- invalid inputs (checked before P4 / top-level shortcuts) ----------

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> policy.nextLevel(null, TECHNICIAN, min(1)));
        assertThrows(IllegalArgumentException.class, () -> policy.nextLevel(Priority.P1, null, min(1)));
        assertThrows(IllegalArgumentException.class, () -> policy.nextLevel(Priority.P1, TECHNICIAN, null));
        assertThrows(IllegalArgumentException.class, () -> policy.nextLevel(Priority.P4, IT_MANAGER, null));
    }

    @Test
    void rejectsNegativeElapsedTimeEvenForP4AndTopLevel() {
        Duration negative = NS.negated();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> policy.nextLevel(Priority.P1, TECHNICIAN, negative));
        assertTrue(e.getMessage().contains("negative"));
        assertThrows(IllegalArgumentException.class, () -> policy.nextLevel(Priority.P4, TECHNICIAN, negative));
        assertThrows(IllegalArgumentException.class, () -> policy.nextLevel(Priority.P1, IT_MANAGER, negative));
    }

    // ---------- configuration ----------

    @Test
    void defaultIntervalsMatchTheTeamTable() {
        TimeBasedEscalationPolicy defaults = new TimeBasedEscalationPolicy();
        assertEquals(Optional.of(min(10)), defaults.intervalFor(Priority.P1));
        assertEquals(Optional.of(min(30)), defaults.intervalFor(Priority.P2));
        assertEquals(Optional.of(hours(2)), defaults.intervalFor(Priority.P3));
        assertEquals(Optional.empty(), defaults.intervalFor(Priority.P4));
    }

    @Test
    void customIntervalsAreUsed() {
        EscalationPolicy custom = new TimeBasedEscalationPolicy(Map.of(
                Priority.P1, min(5), Priority.P2, min(15), Priority.P3, hours(1)));

        assertEquals(TECHNICIAN, custom.nextLevel(Priority.P1, TECHNICIAN, min(5).minus(NS)));
        assertEquals(TEAM_LEAD, custom.nextLevel(Priority.P1, TECHNICIAN, min(5)));
        assertEquals(SYSTEM_ADMIN, custom.nextLevel(Priority.P1, TEAM_LEAD, min(10)));
        assertEquals(IT_MANAGER, custom.nextLevel(Priority.P2, SYSTEM_ADMIN, min(45)));
        assertEquals(SYSTEM_ADMIN, custom.nextLevel(Priority.P3, SYSTEM_ADMIN, hours(3).minus(NS)));
        assertEquals(TECHNICIAN, custom.nextLevel(Priority.P4, TECHNICIAN, hours(100)));
    }

    @Test
    void subSecondIntervalsAreComparedExactly() {
        // Catches a policy that compares whole seconds: 10.2 s must not reach a 10.5 s step.
        Duration interval = Duration.ofMillis(10_500);
        EscalationPolicy custom = new TimeBasedEscalationPolicy(Map.of(
                Priority.P1, interval, Priority.P2, min(15), Priority.P3, hours(1)));

        assertEquals(TECHNICIAN, custom.nextLevel(Priority.P1, TECHNICIAN, Duration.ofMillis(10_200)));
        assertEquals(TECHNICIAN, custom.nextLevel(Priority.P1, TECHNICIAN, interval.minus(NS)));
        assertEquals(TEAM_LEAD, custom.nextLevel(Priority.P1, TECHNICIAN, interval));
        assertEquals(TEAM_LEAD, custom.nextLevel(Priority.P1, TECHNICIAN, interval.plus(NS)));
        assertEquals(TEAM_LEAD, custom.nextLevel(Priority.P1, TEAM_LEAD, Duration.ofMillis(20_999)));
        assertEquals(SYSTEM_ADMIN, custom.nextLevel(Priority.P1, TEAM_LEAD, Duration.ofSeconds(21)));
    }

    @Test
    void customIntervalsAreCopied() {
        Map<Priority, Duration> intervals = new EnumMap<>(Priority.class);
        intervals.put(Priority.P1, min(5));
        intervals.put(Priority.P2, min(15));
        intervals.put(Priority.P3, hours(1));
        TimeBasedEscalationPolicy custom = new TimeBasedEscalationPolicy(intervals);

        intervals.put(Priority.P1, hours(10));
        intervals.clear();

        assertEquals(Optional.of(min(5)), custom.intervalFor(Priority.P1));
        assertEquals(TEAM_LEAD, custom.nextLevel(Priority.P1, TECHNICIAN, min(5)));
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new TimeBasedEscalationPolicy(null));

        // missing P3
        assertThrows(IllegalArgumentException.class, () -> new TimeBasedEscalationPolicy(
                Map.of(Priority.P1, min(5), Priority.P2, min(15))));

        // P4 must not be configured
        IllegalArgumentException p4 = assertThrows(IllegalArgumentException.class,
                () -> new TimeBasedEscalationPolicy(Map.of(Priority.P1, min(5), Priority.P2, min(15),
                        Priority.P3, hours(1), Priority.P4, hours(8))));
        assertTrue(p4.getMessage().contains("P4"));

        // zero and negative intervals
        assertThrows(IllegalArgumentException.class, () -> new TimeBasedEscalationPolicy(
                Map.of(Priority.P1, Duration.ZERO, Priority.P2, min(15), Priority.P3, hours(1))));
        assertThrows(IllegalArgumentException.class, () -> new TimeBasedEscalationPolicy(
                Map.of(Priority.P1, min(-5), Priority.P2, min(15), Priority.P3, hours(1))));

        // null value and null key (HashMap allows both)
        Map<Priority, Duration> nullValue = new HashMap<>(Map.of(Priority.P1, min(5), Priority.P2, min(15)));
        nullValue.put(Priority.P3, null);
        assertThrows(IllegalArgumentException.class, () -> new TimeBasedEscalationPolicy(nullValue));

        Map<Priority, Duration> nullKey = new HashMap<>(
                Map.of(Priority.P1, min(5), Priority.P2, min(15), Priority.P3, hours(1)));
        nullKey.put(null, min(1));
        assertThrows(IllegalArgumentException.class, () -> new TimeBasedEscalationPolicy(nullKey));
    }

    @Test
    void rejectsIntervalTooLargeForThreeSteps() {
        Duration huge = Duration.ofSeconds(Long.MAX_VALUE / 2);
        assertThrows(IllegalArgumentException.class, () -> new TimeBasedEscalationPolicy(
                Map.of(Priority.P1, huge, Priority.P2, min(15), Priority.P3, hours(1))));

        // The largest interval that still fits three times works, including at the top threshold.
        Duration largest = Duration.ofSeconds(Long.MAX_VALUE / 3);
        TimeBasedEscalationPolicy big = new TimeBasedEscalationPolicy(
                Map.of(Priority.P1, largest, Priority.P2, min(15), Priority.P3, hours(1)));
        assertEquals(SYSTEM_ADMIN, big.nextLevel(Priority.P1, SYSTEM_ADMIN, largest.multipliedBy(3).minus(NS)));
        assertEquals(IT_MANAGER, big.nextLevel(Priority.P1, SYSTEM_ADMIN, largest.multipliedBy(3)));
    }

    @Test
    void policyIsStatelessAcrossRepeatedCalls() {
        for (int i = 0; i < 5; i++) {
            expect(TEAM_LEAD, Priority.P1, TECHNICIAN, min(10));
            expect(TEAM_LEAD, Priority.P1, TEAM_LEAD, min(10));
        }
    }
}
