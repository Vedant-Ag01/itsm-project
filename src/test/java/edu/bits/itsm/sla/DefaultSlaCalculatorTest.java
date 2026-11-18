package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Priority;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static edu.bits.itsm.sla.SlaStatus.AT_RISK;
import static edu.bits.itsm.sla.SlaStatus.BREACHED;
import static edu.bits.itsm.sla.SlaStatus.ON_TRACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Every expected time and status below was worked out by hand from the rules:
// both clocks start at creation, warning = creation + 80% of the target (rounded
// down to the nanosecond), AT_RISK up to and including the deadline, BREACHED
// strictly after it, and a finished obligation is judged by when it finished.
// Default table: P1 5m/30m, P2 15m/2h, P3 1h/8h, P4 4h/24h.
class DefaultSlaCalculatorTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 11, 10, 0);
    private static final LocalDateTime NEXT_DAY = LocalDateTime.of(2026, 11, 12, 10, 0);
    private static final Duration NS = Duration.ofNanos(1);

    private final SlaCalculator calculator = new DefaultSlaCalculator(new DefaultSlaPolicy());

    // A time on 2026-11-11 (the day of T0).
    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 11, 11, hour, minute);
    }

    private static LocalDateTime at(int hour, int minute, int second) {
        return LocalDateTime.of(2026, 11, 11, hour, minute, second);
    }

    // Same target for every priority.
    private static Map<Priority, Duration> allPriorities(Duration target) {
        return Map.of(Priority.P1, target, Priority.P2, target,
                Priority.P3, target, Priority.P4, target);
    }

    private static SlaCalculator customCalculator(Duration response, Duration resolution) {
        return new DefaultSlaCalculator(new DefaultSlaPolicy(allPriorities(response),
                allPriorities(resolution)));
    }

    // Test stub: default targets, except P2 returns the given (possibly broken) values.
    private static class BrokenP2Policy implements SlaPolicy {
        private final SlaPolicy defaults = new DefaultSlaPolicy();
        private final Duration p2Response;
        private final Duration p2Resolution;

        BrokenP2Policy(Duration p2Response, Duration p2Resolution) {
            this.p2Response = p2Response;
            this.p2Resolution = p2Resolution;
        }

        @Override
        public Duration responseTarget(Priority priority) {
            return priority == Priority.P2 ? p2Response : defaults.responseTarget(priority);
        }

        @Override
        public Duration resolutionTarget(Priority priority) {
            return priority == Priority.P2 ? p2Resolution : defaults.resolutionTarget(priority);
        }
    }

    // ---- the 15 rows of the brief's section 9 table (P1, created 10:00) ----

    @Test
    void row01_nothingDone_atCreation_onTrack() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P1, null, null, T0));
    }

    @Test
    void row02_justBeforeResponseWarning_onTrack() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P1, null, null, at(10, 4).minus(NS)));
    }

    @Test
    void row03_exactlyResponseWarning_atRisk() {
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, null, null, at(10, 4)));
    }

    @Test
    void row04_exactlyResponseDeadline_atRisk() {
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, null, null, at(10, 5)));
    }

    @Test
    void row05_justAfterResponseDeadline_breached() {
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, null, null, at(10, 5).plus(NS)));
    }

    @Test
    void row06_acked_justBeforeResolutionWarning_onTrack() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P1, at(10, 4), null, at(10, 24).minus(NS)));
    }

    @Test
    void row07_acked_exactlyResolutionWarning_atRisk() {
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, at(10, 4), null, at(10, 24)));
    }

    @Test
    void row08_acked_exactlyResolutionDeadline_atRisk() {
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, at(10, 4), null, at(10, 30)));
    }

    @Test
    void row09_acked_justAfterResolutionDeadline_breached() {
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, at(10, 4), null, at(10, 30).plus(NS)));
    }

    @Test
    void row10_bothOnTime_pastWarningPoint_onTrack() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P1, at(10, 4), at(10, 29), NEXT_DAY));
    }

    @Test
    void row11_bothExactlyAtDeadlines_onTrack() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P1, at(10, 5), at(10, 30), NEXT_DAY));
    }

    @Test
    void row12_resolvedOneNanosecondLate_breachedForever() {
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, at(10, 4), at(10, 30).plus(NS), NEXT_DAY));
    }

    @Test
    void row13_lateAckNotErasedByTimelyResolution_breached() {
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, at(10, 6), at(10, 20), NEXT_DAY));
    }

    @Test
    void row14_ackedAt1003_now1025_atRisk() {
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, at(10, 3), null, at(10, 25)));
    }

    @Test
    void row15_ackDoesNotMoveResolutionDeadline_breached() {
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, at(10, 3), null, at(10, 33)));
    }

    // ---- extra P1 cases ----

    @Test
    void ackedAtWarningPoint_noStaleWarning_onTrack() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P1, at(10, 4), null, at(10, 4)));
    }

    @Test
    void ackedOneNanosecondLate_breached() {
        LocalDateTime late = at(10, 5).plus(NS);
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, late, null, late));
    }

    @Test
    void responseBreachBeatsResolutionWarning() {
        // 10:24 is the resolution warning point, but response (due 10:05) is missed.
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, null, null, at(10, 24)));
    }

    @Test
    void everythingAtCreationTime_onTrack() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P1, T0, T0, T0));
    }

    // ---- D5: rejected arguments ----

    @Test
    void evaluateRejectsNullCreatedAtPriorityOrNow() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(null, Priority.P1, null, null, T0));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(T0, null, null, null, T0));
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(T0, Priority.P1, null, null, null));
    }

    @Test
    void deadlinesRejectNullCreatedAtOrPriority() {
        assertThrows(IllegalArgumentException.class, () -> calculator.responseDeadline(null, Priority.P1));
        assertThrows(IllegalArgumentException.class, () -> calculator.responseDeadline(T0, null));
        assertThrows(IllegalArgumentException.class, () -> calculator.resolutionDeadline(null, Priority.P1));
        assertThrows(IllegalArgumentException.class, () -> calculator.resolutionDeadline(T0, null));
    }

    @Test
    void rejectsNowBeforeCreation() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(T0, Priority.P1, null, null,
                        LocalDateTime.of(2026, 11, 11, 9, 59, 59, 999_999_999)));
    }

    @Test
    void rejectsAckBeforeCreation() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(T0, Priority.P1,
                        LocalDateTime.of(2026, 11, 11, 9, 59, 59, 999_999_999), null, at(10, 1)));
    }

    @Test
    void rejectsResolvedBeforeCreation() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(T0, Priority.P1, null,
                        LocalDateTime.of(2026, 11, 11, 9, 59, 59, 999_999_999), at(10, 1)));
    }

    @Test
    void rejectsAckAfterNow() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(T0, Priority.P1, at(10, 2), null, at(10, 1)));
    }

    @Test
    void rejectsResolvedAfterNow() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(T0, Priority.P1, at(10, 1), at(10, 3), at(10, 2)));
    }

    @Test
    void rejectsResolvedWithoutAck() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(T0, Priority.P1, null, at(10, 2), at(10, 3)));
    }

    @Test
    void rejectsResolvedBeforeAck() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(T0, Priority.P1, at(10, 3), at(10, 2), at(10, 4)));
    }

    // ---- deadlines for all priorities (creation + full target) ----

    @Test
    void deadlinesForAllPriorities() {
        assertEquals(at(10, 5), calculator.responseDeadline(T0, Priority.P1));
        assertEquals(at(10, 30), calculator.resolutionDeadline(T0, Priority.P1));
        assertEquals(at(10, 15), calculator.responseDeadline(T0, Priority.P2));
        assertEquals(at(12, 0), calculator.resolutionDeadline(T0, Priority.P2));
        assertEquals(at(11, 0), calculator.responseDeadline(T0, Priority.P3));
        assertEquals(at(18, 0), calculator.resolutionDeadline(T0, Priority.P3));
        assertEquals(at(14, 0), calculator.responseDeadline(T0, Priority.P4));
        assertEquals(NEXT_DAY, calculator.resolutionDeadline(T0, Priority.P4));
    }

    // ---- warning points: 1 ns before warning / warning / deadline / 1 ns after ----
    // Resolution cases use an early acknowledgement (10:01) so response is met.

    @Test
    void p1ResponseBoundaries() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P1, null, null, at(10, 4).minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, null, null, at(10, 4)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, null, null, at(10, 5)));
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, null, null, at(10, 5).plus(NS)));
    }

    @Test
    void p1ResolutionBoundaries() {
        LocalDateTime ack = at(10, 1);
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P1, ack, null, at(10, 24).minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, ack, null, at(10, 24)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, ack, null, at(10, 30)));
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, ack, null, at(10, 30).plus(NS)));
    }

    @Test
    void p2ResponseBoundaries() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P2, null, null, at(10, 12).minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P2, null, null, at(10, 12)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P2, null, null, at(10, 15)));
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P2, null, null, at(10, 15).plus(NS)));
    }

    @Test
    void p2ResolutionBoundaries() {
        LocalDateTime ack = at(10, 1);
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P2, ack, null, at(11, 36).minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P2, ack, null, at(11, 36)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P2, ack, null, at(12, 0)));
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P2, ack, null, at(12, 0).plus(NS)));
    }

    @Test
    void p3ResponseBoundaries() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P3, null, null, at(10, 48).minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P3, null, null, at(10, 48)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P3, null, null, at(11, 0)));
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P3, null, null, at(11, 0).plus(NS)));
    }

    @Test
    void p3ResolutionBoundaries() {
        LocalDateTime ack = at(10, 1);
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P3, ack, null, at(16, 24).minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P3, ack, null, at(16, 24)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P3, ack, null, at(18, 0)));
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P3, ack, null, at(18, 0).plus(NS)));
    }

    @Test
    void p4ResponseBoundaries() {
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P4, null, null, at(13, 12).minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P4, null, null, at(13, 12)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P4, null, null, at(14, 0)));
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P4, null, null, at(14, 0).plus(NS)));
    }

    @Test
    void p4ResolutionBoundaries() {
        LocalDateTime ack = at(10, 1);
        LocalDateTime warning = LocalDateTime.of(2026, 11, 12, 5, 12);
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P4, ack, null, warning.minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P4, ack, null, warning));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P4, ack, null, NEXT_DAY));
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P4, ack, null, NEXT_DAY.plus(NS)));
    }

    // ---- crossing midnight, month, year and a leap day ----

    @Test
    void p1CrossingMidnight() {
        LocalDateTime created = LocalDateTime.of(2026, 11, 11, 23, 58);
        assertEquals(LocalDateTime.of(2026, 11, 12, 0, 3), calculator.responseDeadline(created, Priority.P1));
        assertEquals(LocalDateTime.of(2026, 11, 12, 0, 28), calculator.resolutionDeadline(created, Priority.P1));

        // Acknowledged after midnight but before 00:03: response met.
        LocalDateTime ack = LocalDateTime.of(2026, 11, 12, 0, 2, 30);
        LocalDateTime warning = LocalDateTime.of(2026, 11, 12, 0, 22);
        assertEquals(ON_TRACK, calculator.evaluate(created, Priority.P1, ack, null, warning.minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(created, Priority.P1, ack, null, warning));
        assertEquals(AT_RISK, calculator.evaluate(created, Priority.P1, ack, null,
                LocalDateTime.of(2026, 11, 12, 0, 28)));
        assertEquals(BREACHED, calculator.evaluate(created, Priority.P1, ack, null,
                LocalDateTime.of(2026, 11, 12, 0, 28).plus(NS)));
    }

    @Test
    void p3CrossingMonthEnd() {
        LocalDateTime created = LocalDateTime.of(2026, 11, 30, 20, 0);
        assertEquals(LocalDateTime.of(2026, 12, 1, 4, 0), calculator.resolutionDeadline(created, Priority.P3));

        LocalDateTime ack = LocalDateTime.of(2026, 11, 30, 20, 10);
        LocalDateTime warning = LocalDateTime.of(2026, 12, 1, 2, 24);
        assertEquals(ON_TRACK, calculator.evaluate(created, Priority.P3, ack, null, warning.minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(created, Priority.P3, ack, null, warning));
    }

    @Test
    void p4CrossingYearEnd() {
        LocalDateTime created = LocalDateTime.of(2026, 12, 31, 23, 30);
        LocalDateTime responseDue = LocalDateTime.of(2027, 1, 1, 3, 30);
        assertEquals(responseDue, calculator.responseDeadline(created, Priority.P4));
        assertEquals(LocalDateTime.of(2027, 1, 1, 23, 30), calculator.resolutionDeadline(created, Priority.P4));

        LocalDateTime responseWarning = LocalDateTime.of(2027, 1, 1, 2, 42);
        assertEquals(ON_TRACK, calculator.evaluate(created, Priority.P4, null, null, responseWarning.minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(created, Priority.P4, null, null, responseWarning));

        // Acknowledged exactly at 03:30: on time; resolution warning is 18:42.
        LocalDateTime resolutionWarning = LocalDateTime.of(2027, 1, 1, 18, 42);
        assertEquals(ON_TRACK, calculator.evaluate(created, Priority.P4, responseDue, null,
                resolutionWarning.minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(created, Priority.P4, responseDue, null, resolutionWarning));

        // Acknowledged one nanosecond after 03:30: response missed.
        assertEquals(BREACHED, calculator.evaluate(created, Priority.P4, responseDue.plus(NS), null,
                LocalDateTime.of(2027, 1, 1, 3, 31)));
    }

    @Test
    void p4LeapDay() {
        LocalDateTime created = LocalDateTime.of(2028, 2, 28, 12, 0);
        assertEquals(LocalDateTime.of(2028, 2, 29, 12, 0), calculator.resolutionDeadline(created, Priority.P4));
    }

    @Test
    void creationTimeWithNanosecondsIsKept() {
        LocalDateTime created = LocalDateTime.of(2026, 11, 11, 10, 0, 0, 123_456_789);
        LocalDateTime warning = LocalDateTime.of(2026, 11, 11, 10, 4, 0, 123_456_789);
        assertEquals(LocalDateTime.of(2026, 11, 11, 10, 5, 0, 123_456_789),
                calculator.responseDeadline(created, Priority.P1));
        assertEquals(ON_TRACK, calculator.evaluate(created, Priority.P1, null, null, warning.minus(NS)));
        assertEquals(AT_RISK, calculator.evaluate(created, Priority.P1, null, null, warning));
    }

    // ---- custom and tiny targets (80% rounded down to the nanosecond) ----

    @Test
    void customSevenMinuteResponseWarnsAt5m36s() {
        SlaCalculator custom = customCalculator(Duration.ofMinutes(7), Duration.ofHours(1));
        assertEquals(ON_TRACK, custom.evaluate(T0, Priority.P1, null, null,
                LocalDateTime.of(2026, 11, 11, 10, 5, 35, 999_999_999)));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, at(10, 5, 36)));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, at(10, 7)));
        assertEquals(BREACHED, custom.evaluate(T0, Priority.P1, null, null, at(10, 7).plus(NS)));
    }

    @Test
    void oneNanosecondTarget() {
        // 80% of 1 ns = 0.8 ns, rounded down to 0: AT_RISK from creation.
        SlaCalculator custom = customCalculator(Duration.ofNanos(1), Duration.ofHours(1));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, T0));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(1)));
        assertEquals(BREACHED, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(2)));
    }

    @Test
    void threeNanosecondTarget() {
        // 80% of 3 ns = 2.4 ns, rounded down to 2 ns.
        SlaCalculator custom = customCalculator(Duration.ofNanos(3), Duration.ofHours(1));
        assertEquals(ON_TRACK, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(1)));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(2)));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(3)));
        assertEquals(BREACHED, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(4)));
    }

    @Test
    void threeNanosecondEqualTargets() {
        // Response = resolution = 3 ns; acknowledged at creation, so only resolution runs.
        SlaCalculator custom = customCalculator(Duration.ofNanos(3), Duration.ofNanos(3));
        assertEquals(ON_TRACK, custom.evaluate(T0, Priority.P1, T0, null, T0.plusNanos(1)));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, T0, null, T0.plusNanos(2)));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, T0, null, T0.plusNanos(3)));
        assertEquals(BREACHED, custom.evaluate(T0, Priority.P1, T0, null, T0.plusNanos(4)));
    }

    @Test
    void sevenNanosecondTarget() {
        // 80% of 7 ns = 5.6 ns, rounded down to 5 ns.
        SlaCalculator custom = customCalculator(Duration.ofNanos(7), Duration.ofHours(1));
        assertEquals(ON_TRACK, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(4)));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(5)));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(7)));
        assertEquals(BREACHED, custom.evaluate(T0, Priority.P1, null, null, T0.plusNanos(8)));
    }

    @Test
    void warningOffsetLiterals() {
        assertEquals(Duration.ofMinutes(4), DefaultSlaCalculator.warningOffset(Duration.ofMinutes(5)));
        assertEquals(Duration.ofMinutes(24), DefaultSlaCalculator.warningOffset(Duration.ofMinutes(30)));
        assertEquals(Duration.ofNanos(5), DefaultSlaCalculator.warningOffset(Duration.ofNanos(7)));
        assertEquals(Duration.ofNanos(2), DefaultSlaCalculator.warningOffset(Duration.ofNanos(3)));
        assertEquals(Duration.ZERO, DefaultSlaCalculator.warningOffset(Duration.ofNanos(1)));
    }

    @Test
    void yearLongTargetPlusSevenNanoseconds() {
        // 365 days + 7 ns: warning = 292 days + 5 ns, deadline = 365 days + 7 ns.
        Duration target = Duration.ofDays(365).plusNanos(7);
        SlaCalculator custom = customCalculator(target, target);
        LocalDateTime warning = LocalDateTime.of(2027, 8, 30, 10, 0, 0, 5);
        LocalDateTime deadline = LocalDateTime.of(2027, 11, 11, 10, 0, 0, 7);
        assertEquals(deadline, custom.responseDeadline(T0, Priority.P1));
        assertEquals(deadline, custom.resolutionDeadline(T0, Priority.P1));
        assertEquals(ON_TRACK, custom.evaluate(T0, Priority.P1, null, null, warning.minus(NS)));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, warning));
        assertEquals(AT_RISK, custom.evaluate(T0, Priority.P1, null, null, deadline));
        assertEquals(BREACHED, custom.evaluate(T0, Priority.P1, null, null, deadline.plus(NS)));
    }

    // ---- date-range overflow becomes IllegalArgumentException with a cause ----

    @Test
    void evaluateOverflowIsRejected() {
        LocalDateTime nearEnd = LocalDateTime.of(999999999, 12, 31, 23, 40);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(nearEnd, Priority.P1, null, null, nearEnd));
        assertNotNull(e.getCause());
    }

    @Test
    void evaluateAlwaysComputesBothDeadlines() {
        // Response (due 23:45) is already breached at 23:46, but the resolution
        // deadline (23:40 + 30 minutes) is past the end of the date range.
        LocalDateTime nearEnd = LocalDateTime.of(999999999, 12, 31, 23, 40);
        LocalDateTime now = LocalDateTime.of(999999999, 12, 31, 23, 46);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> calculator.evaluate(nearEnd, Priority.P1, null, null, now));
        assertNotNull(e.getCause());
    }

    @Test
    void resolutionDeadlineOverflowIsRejected() {
        LocalDateTime nearEnd = LocalDateTime.of(999999999, 12, 31, 23, 40);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> calculator.resolutionDeadline(nearEnd, Priority.P1));
        assertNotNull(e.getCause());
    }

    @Test
    void responseDeadlineOverflowIsRejected() {
        LocalDateTime nearEnd = LocalDateTime.of(999999999, 12, 31, 23, 56);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> calculator.responseDeadline(nearEnd, Priority.P1));
        assertNotNull(e.getCause());
    }

    @Test
    void hugeTargetIsAcceptedButCannotBeAdded() {
        SlaCalculator huge = customCalculator(Duration.ofMinutes(5), Duration.ofSeconds(Long.MAX_VALUE));
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> huge.resolutionDeadline(T0, Priority.P1));
        assertNotNull(e1.getCause());
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> huge.evaluate(T0, Priority.P1, null, null, T0));
        assertNotNull(e2.getCause());
    }

    // ---- D4: a broken custom policy gives IllegalStateException ----

    private static void assertBrokenForP2Only(SlaPolicy policy) {
        SlaCalculator broken = new DefaultSlaCalculator(policy);
        assertThrows(IllegalStateException.class, () -> broken.responseDeadline(T0, Priority.P2));
        assertThrows(IllegalStateException.class, () -> broken.resolutionDeadline(T0, Priority.P2));
        assertThrows(IllegalStateException.class,
                () -> broken.evaluate(T0, Priority.P2, null, null, T0));
        // P1 still works.
        assertEquals(at(10, 5), broken.responseDeadline(T0, Priority.P1));
        assertEquals(at(10, 30), broken.resolutionDeadline(T0, Priority.P1));
        assertEquals(ON_TRACK, broken.evaluate(T0, Priority.P1, null, null, T0));
    }

    @Test
    void nullResponseTargetIsMissingConfiguration() {
        assertBrokenForP2Only(new BrokenP2Policy(null, Duration.ofHours(2)));
        SlaCalculator broken = new DefaultSlaCalculator(new BrokenP2Policy(null, Duration.ofHours(2)));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> broken.responseDeadline(T0, Priority.P2));
        assertTrue(e.getMessage().contains("missing SLA configuration"));
        assertTrue(e.getMessage().contains("P2"));
    }

    @Test
    void nullResolutionTargetIsMissingConfiguration() {
        assertBrokenForP2Only(new BrokenP2Policy(Duration.ofMinutes(15), null));
    }

    @Test
    void zeroTargetIsBroken() {
        assertBrokenForP2Only(new BrokenP2Policy(Duration.ZERO, Duration.ofHours(2)));
    }

    @Test
    void negativeTargetIsBroken() {
        assertBrokenForP2Only(new BrokenP2Policy(Duration.ofMinutes(15), Duration.ofMinutes(-1)));
    }

    @Test
    void responseLongerThanResolutionIsBroken() {
        assertBrokenForP2Only(new BrokenP2Policy(Duration.ofMinutes(10), Duration.ofMinutes(5)));
        SlaCalculator broken = new DefaultSlaCalculator(
                new BrokenP2Policy(Duration.ofMinutes(10), Duration.ofMinutes(5)));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> broken.evaluate(T0, Priority.P2, null, null, T0));
        assertTrue(e.getMessage().contains("P2"));
    }

    @Test
    void brokenPolicyIsReportedBeforeDateRangeProblem() {
        // Adding 10 minutes to 23:56 would also leave the date range, but the
        // broken policy is checked first.
        SlaCalculator broken = new DefaultSlaCalculator(
                new BrokenP2Policy(Duration.ofMinutes(10), Duration.ofMinutes(5)));
        LocalDateTime nearEnd = LocalDateTime.of(999999999, 12, 31, 23, 56);
        assertThrows(IllegalStateException.class, () -> broken.responseDeadline(nearEnd, Priority.P2));
        assertThrows(IllegalStateException.class,
                () -> broken.evaluate(nearEnd, Priority.P2, null, null, nearEnd));
    }

    @Test
    void equalTargetsFromCustomPolicyAreAccepted() {
        SlaCalculator equal = new DefaultSlaCalculator(
                new BrokenP2Policy(Duration.ofMinutes(10), Duration.ofMinutes(10)));
        assertEquals(at(10, 10), equal.responseDeadline(T0, Priority.P2));
        assertEquals(at(10, 10), equal.resolutionDeadline(T0, Priority.P2));
        assertEquals(AT_RISK, equal.evaluate(T0, Priority.P2, null, null, at(10, 8)));
    }

    @Test
    void callerArgumentsAreCheckedBeforeThePolicy() {
        SlaCalculator broken = new DefaultSlaCalculator(new BrokenP2Policy(null, null));
        assertThrows(IllegalArgumentException.class, () -> broken.responseDeadline(T0, null));
        assertThrows(IllegalArgumentException.class, () -> broken.resolutionDeadline(T0, null));
        assertThrows(IllegalArgumentException.class, () -> broken.evaluate(T0, null, null, null, T0));
        // Bad timestamps for the broken P2 are still reported as bad arguments.
        assertThrows(IllegalArgumentException.class,
                () -> broken.evaluate(T0, Priority.P2, null, null, T0.minus(NS)));
    }

    @Test
    void constructorRejectsNullPolicy() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultSlaCalculator(null));
    }

    // ---- substitution, priority change, determinism ----

    @Test
    void injectedPolicyChangesDeadlines() {
        // Test stub: 1 hour to respond, 2 hours to resolve, for every priority.
        SlaPolicy stub = new SlaPolicy() {
            @Override
            public Duration responseTarget(Priority priority) {
                return Duration.ofHours(1);
            }

            @Override
            public Duration resolutionTarget(Priority priority) {
                return Duration.ofHours(2);
            }
        };
        SlaCalculator custom = new DefaultSlaCalculator(stub);
        assertEquals(at(11, 0), custom.responseDeadline(T0, Priority.P1));
        assertEquals(at(12, 0), custom.resolutionDeadline(T0, Priority.P1));
        assertEquals(ON_TRACK, custom.evaluate(T0, Priority.P1, null, null, at(10, 6)));
    }

    @Test
    void changedPriorityIsRecalculated() {
        // No acknowledgement, checked at 10:06.
        assertEquals(BREACHED, calculator.evaluate(T0, Priority.P1, null, null, at(10, 6)));
        assertEquals(ON_TRACK, calculator.evaluate(T0, Priority.P2, null, null, at(10, 6)));
    }

    @Test
    void sameInputsGiveSameResult() {
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, at(10, 3), null, at(10, 25)));
        assertEquals(AT_RISK, calculator.evaluate(T0, Priority.P1, at(10, 3), null, at(10, 25)));
        assertEquals(at(10, 30), calculator.resolutionDeadline(T0, Priority.P1));
        assertEquals(at(10, 30), calculator.resolutionDeadline(T0, Priority.P1));
    }
}
