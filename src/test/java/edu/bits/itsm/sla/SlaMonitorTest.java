package edu.bits.itsm.sla;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.Urgency;
import edu.bits.itsm.sla.SlaMonitor.SkippedIncident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Expected alerts are worked out by hand. P1 created 10:00 (default table):
// response warning 10:04, response due 10:05, resolution warning 10:24, due 10:30.
// Each test builds ONE monitor and reuses it, like the real scheduler would.
class SlaMonitorTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 11, 10, 0);

    private static final Employee REPORTER = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private static final ItService SERVICE = new ItService("Payment application", BusinessCriticality.HIGH);
    private static final Technician TECHNICIAN = new Technician("T1", "Ravi", "ravi@bits.edu");

    private final SlaCalculator calculator = new DefaultSlaCalculator(new DefaultSlaPolicy());
    private final SlaMonitor monitor = new SlaMonitor(calculator);

    // ---------- time helpers (all on 2026-11-11) ----------

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 11, 11, hour, minute);
    }

    private static LocalDateTime at(int hour, int minute, int second) {
        return LocalDateTime.of(2026, 11, 11, hour, minute, second);
    }

    // ---------- incident fixtures ----------

    // Test-only use of Incident.restore: it rebuilds a saved snapshot with fixed
    // timestamps, which normal lifecycle methods cannot do (they read the clock).
    private static Incident restore(String id, IncidentStatus status, Priority priority,
                                    LocalDateTime createdAt, LocalDateTime acknowledgedAt,
                                    LocalDateTime resolvedAt) {
        String resolutionNote = resolvedAt == null ? null : "Restarted the payment service";
        boolean closed = status == IncidentStatus.CLOSED;
        return Incident.restore(id, REPORTER, "Payment down", "Payments are failing",
                SERVICE, Impact.HIGH, Urgency.HIGH, 50, createdAt, status,
                IncidentCategory.APPLICATION, priority, TECHNICIAN,
                acknowledgedAt, resolvedAt, closed ? resolvedAt : null,
                resolutionNote, closed ? "Confirmed by user" : null, new ArrayList<>(), null);
    }

    // Assigned, not acknowledged yet, created at 10:00.
    private static Incident unacked(String id, Priority priority) {
        return restore(id, IncidentStatus.ASSIGNED, priority, T0, null, null);
    }

    // Acknowledged, work in progress, created at 10:00.
    private static Incident acked(String id, Priority priority, LocalDateTime acknowledgedAt) {
        return restore(id, IncidentStatus.IN_PROGRESS, priority, T0, acknowledgedAt, null);
    }

    // Resolved, created at 10:00.
    private static Incident resolved(String id, Priority priority, LocalDateTime acknowledgedAt,
                                     LocalDateTime resolvedAt) {
        return restore(id, IncidentStatus.RESOLVED, priority, T0, acknowledgedAt, resolvedAt);
    }

    private static void assertAlert(SlaAlert alert, String id, SlaAlert.Type type) {
        assertEquals(id, alert.incidentId());
        assertEquals(type, alert.type());
        assertTrue(alert.message().contains(id));
    }

    private static void assertOneAlert(List<SlaAlert> alerts, String id, SlaAlert.Type type) {
        assertEquals(1, alerts.size());
        assertAlert(alerts.get(0), id, type);
    }

    // ---------- test stubs ----------

    // Uses the real calculator, but can be switched to fail for P2 incidents.
    private static class SwitchableCalculator implements SlaCalculator {
        private final SlaCalculator real = new DefaultSlaCalculator(new DefaultSlaPolicy());
        private boolean failForP2;

        @Override
        public LocalDateTime responseDeadline(LocalDateTime createdAt, Priority priority) {
            return real.responseDeadline(createdAt, priority);
        }

        @Override
        public LocalDateTime resolutionDeadline(LocalDateTime createdAt, Priority priority) {
            return real.resolutionDeadline(createdAt, priority);
        }

        @Override
        public SlaStatus evaluate(LocalDateTime createdAt, Priority priority,
                                  LocalDateTime acknowledgedAt, LocalDateTime resolvedAt,
                                  LocalDateTime now) {
            if (failForP2 && priority == Priority.P2) {
                throw new IllegalStateException("test stub failure");
            }
            return real.evaluate(createdAt, priority, acknowledgedAt, resolvedAt, now);
        }
    }

    // Default targets, but can be switched to return no targets for P2.
    private static class SwitchablePolicy implements SlaPolicy {
        private final SlaPolicy defaults = new DefaultSlaPolicy();
        private boolean brokenForP2;

        @Override
        public Duration responseTarget(Priority priority) {
            return brokenForP2 && priority == Priority.P2 ? null : defaults.responseTarget(priority);
        }

        @Override
        public Duration resolutionTarget(Priority priority) {
            return brokenForP2 && priority == Priority.P2 ? null : defaults.resolutionTarget(priority);
        }
    }

    // Always returns the given status (may be null).
    private static SlaCalculator fixedStatusCalculator(SlaStatus status) {
        return new SlaCalculator() {
            @Override
            public LocalDateTime responseDeadline(LocalDateTime createdAt, Priority priority) {
                return createdAt;
            }

            @Override
            public LocalDateTime resolutionDeadline(LocalDateTime createdAt, Priority priority) {
                return createdAt;
            }

            @Override
            public SlaStatus evaluate(LocalDateTime createdAt, Priority priority,
                                      LocalDateTime acknowledgedAt, LocalDateTime resolvedAt,
                                      LocalDateTime now) {
                return status;
            }
        };
    }

    // ---------- basics ----------

    @Test
    void constructorRejectsNullCalculator() {
        assertThrows(IllegalArgumentException.class, () -> new SlaMonitor(null));
    }

    @Test
    void emptyListGivesNoAlertsAndNoSkips() {
        assertEquals(List.of(), monitor.check(List.of(), T0));
        assertEquals(List.of(), monitor.lastSkipped());
    }

    @Test
    void onTrackGivesNoAlert() {
        assertEquals(List.of(), monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 2)));
    }

    // ---------- Option B: alert only on a change ----------

    @Test
    void optionBSequenceForUnacknowledgedP1() {
        List<Incident> incidents = List.of(unacked("INC1", Priority.P1));

        assertOneAlert(monitor.check(incidents, at(10, 4)), "INC1", SlaAlert.Type.WARNING);
        assertEquals(List.of(), monitor.check(incidents, at(10, 5)));
        assertOneAlert(monitor.check(incidents, at(10, 6)), "INC1", SlaAlert.Type.BREACH);
        assertEquals(List.of(), monitor.check(incidents, at(10, 7)));

        // The calculator still reports the real status; only the alerts are silent.
        assertEquals(SlaStatus.AT_RISK, calculator.evaluate(T0, Priority.P1, null, null, at(10, 5)));
        assertEquals(SlaStatus.BREACHED, calculator.evaluate(T0, Priority.P1, null, null, at(10, 7)));
    }

    @Test
    void warningMessageIsExact() {
        List<SlaAlert> alerts = monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4));
        assertEquals(List.of(new SlaAlert("INC1", SlaAlert.Type.WARNING,
                "SLA warning for incident INC1 (P1) found at 2026-11-11T10:04: it has reached "
                        + "the 80% warning point of its response or resolution time.")), alerts);
    }

    @Test
    void breachMessageIsExact() {
        List<SlaAlert> alerts = monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 6));
        assertEquals(List.of(new SlaAlert("INC1", SlaAlert.Type.BREACH,
                "SLA breach for incident INC1 (P1) found at 2026-11-11T10:06: "
                        + "a response or resolution deadline was missed.")), alerts);
    }

    @Test
    void firstObservationAlreadyBreachedGivesOnlyBreach() {
        assertOneAlert(monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 6)),
                "INC1", SlaAlert.Type.BREACH);
    }

    @Test
    void onTrackStraightToBreachedGivesOneBreach() {
        List<Incident> incidents = List.of(unacked("INC1", Priority.P1));
        assertEquals(List.of(), monitor.check(incidents, at(10, 2)));
        assertOneAlert(monitor.check(incidents, at(10, 6)), "INC1", SlaAlert.Type.BREACH);
    }

    @Test
    void separateIdsKeepInputOrder() {
        // B was created at 09:50, so its response was due 09:55: breached at 10:04.
        Incident b = restore("B", IncidentStatus.ASSIGNED, Priority.P1,
                LocalDateTime.of(2026, 11, 11, 9, 50), null, null);
        Incident a = unacked("A", Priority.P1);

        List<SlaAlert> alerts = monitor.check(List.of(b, a), at(10, 4));

        assertEquals(2, alerts.size());
        assertAlert(alerts.get(0), "B", SlaAlert.Type.BREACH);
        assertAlert(alerts.get(1), "A", SlaAlert.Type.WARNING);
    }

    @Test
    void recreatedIncidentObjectsWithSameIdShareMemory() {
        assertOneAlert(monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4)),
                "INC1", SlaAlert.Type.WARNING);
        // A brand-new object loaded again from storage, same ID.
        assertEquals(List.of(), monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 5)));
    }

    @Test
    void reEntryIntoAtRiskWarnsAgain() {
        assertOneAlert(monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4)),
                "INC1", SlaAlert.Type.WARNING);

        // Acknowledged at 10:04:30: response met, back to ON_TRACK.
        List<Incident> ackedCopy = List.of(acked("INC1", Priority.P1, at(10, 4, 30)));
        assertEquals(List.of(), monitor.check(ackedCopy, at(10, 4, 30)));
        assertEquals(List.of(), monitor.check(ackedCopy,
                LocalDateTime.of(2026, 11, 11, 10, 23, 59, 999_999_999)));
        assertOneAlert(monitor.check(ackedCopy, at(10, 24)), "INC1", SlaAlert.Type.WARNING);
        assertEquals(List.of(), monitor.check(ackedCopy, at(10, 30)));
        assertOneAlert(monitor.check(ackedCopy, at(10, 30).plusNanos(1)), "INC1", SlaAlert.Type.BREACH);
    }

    @Test
    void absenceDoesNotForget() {
        assertOneAlert(monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4)),
                "INC1", SlaAlert.Type.WARNING);
        assertEquals(List.of(), monitor.check(List.of(), at(10, 4, 30)));
        assertEquals(List.of(), monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4, 45)));
    }

    @Test
    void priorityChangeIsRecalculated() {
        // As P3 at 10:06 it is ON_TRACK (response warning 10:48).
        assertEquals(List.of(), monitor.check(List.of(unacked("INC1", Priority.P3)), at(10, 6)));
        // Raised to P1: response was due 10:05.
        assertOneAlert(monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 7)),
                "INC1", SlaAlert.Type.BREACH);
    }

    // ---------- completed incidents (D14, D15) ----------

    @Test
    void untrackedCompletedIncidentIsIgnoredSilently() {
        // Acknowledged late at 10:10, resolved late at 10:40.
        Incident done = resolved("INC1", Priority.P1, at(10, 10), at(10, 40));

        assertEquals(List.of(), monitor.check(List.of(done), at(11, 0)));
        assertEquals(List.of(), monitor.lastSkipped());
        // The calculator still says BREACHED when asked directly.
        assertEquals(SlaStatus.BREACHED,
                calculator.evaluate(T0, Priority.P1, at(10, 10), at(10, 40), at(11, 0)));
    }

    @Test
    void untrackedCompletedWithMissingPriorityIsIgnoredNotSkipped() {
        // Completed and untracked is filtered out before the priority check.
        Incident done = resolved("INC1", null, at(10, 1), at(10, 2));

        assertEquals(List.of(), monitor.check(List.of(done), at(10, 5)));
        assertEquals(List.of(), monitor.lastSkipped());
    }

    @Test
    void finalCheckReportsLateResolutionThenForgets() {
        assertOneAlert(monitor.check(List.of(acked("INC1", Priority.P1, at(10, 3))), at(10, 24)),
                "INC1", SlaAlert.Type.WARNING);
        assertEquals(List.of(), monitor.check(List.of(acked("INC1", Priority.P1, at(10, 3))), at(10, 30)));

        // Resolved late at 10:30:20, seen at 10:31.
        Incident done = resolved("INC1", Priority.P1, at(10, 3), at(10, 30, 20));
        assertOneAlert(monitor.check(List.of(done), at(10, 31)), "INC1", SlaAlert.Type.BREACH);
        assertEquals(List.of(), monitor.check(List.of(done), at(10, 32)));
        assertEquals(List.of(), monitor.lastSkipped());

        // Reopened and re-prioritised to P2: resolution warning 11:36. If the monitor
        // still remembered AT_RISK this would be silent; it was forgotten, so it warns.
        assertOneAlert(monitor.check(List.of(acked("INC1", Priority.P2, at(10, 3))), at(11, 36)),
                "INC1", SlaAlert.Type.WARNING);
    }

    @Test
    void finalCheckFromOnTrackReportsLateResolution() {
        assertEquals(List.of(), monitor.check(List.of(acked("INC1", Priority.P1, at(10, 3))), at(10, 10)));
        Incident done = resolved("INC1", Priority.P1, at(10, 3), at(10, 31));
        assertOneAlert(monitor.check(List.of(done), at(10, 35)), "INC1", SlaAlert.Type.BREACH);
        assertEquals(List.of(), monitor.check(List.of(done), at(10, 36)));
    }

    @Test
    void finalCheckDoesNotRepeatBreach() {
        assertOneAlert(monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 6)),
                "INC1", SlaAlert.Type.BREACH);
        // Acknowledged late at 10:07, resolved 10:20: still BREACHED, already alerted.
        assertEquals(List.of(), monitor.check(
                List.of(resolved("INC1", Priority.P1, at(10, 7), at(10, 20))), at(10, 21)));

        // Forgotten: a live copy that is still BREACHED alerts again as a first sighting.
        assertOneAlert(monitor.check(List.of(acked("INC1", Priority.P1, at(10, 7))), at(10, 22)),
                "INC1", SlaAlert.Type.BREACH);
    }

    @Test
    void finalCheckOnTimeIsSilentAndForgets() {
        assertOneAlert(monitor.check(List.of(acked("INC1", Priority.P1, at(10, 3))), at(10, 24)),
                "INC1", SlaAlert.Type.WARNING);
        assertEquals(List.of(), monitor.check(
                List.of(resolved("INC1", Priority.P1, at(10, 3), at(10, 25))), at(10, 26)));

        // Forgotten: if AT_RISK were still remembered this would be silent.
        assertOneAlert(monitor.check(List.of(acked("INC1", Priority.P1, at(10, 3))), at(10, 27)),
                "INC1", SlaAlert.Type.WARNING);
    }

    @Test
    void verifiedBehavesAsResolved() {
        assertOneAlert(monitor.check(List.of(acked("INC1", Priority.P1, at(10, 3))), at(10, 24)),
                "INC1", SlaAlert.Type.WARNING);
        Incident verified = restore("INC1", IncidentStatus.VERIFIED, Priority.P1, T0, at(10, 3), at(10, 31));
        Incident untracked = restore("INC2", IncidentStatus.VERIFIED, Priority.P1, T0, at(10, 10), at(10, 31));

        List<SlaAlert> alerts = monitor.check(List.of(verified, untracked), at(10, 40));

        assertOneAlert(alerts, "INC1", SlaAlert.Type.BREACH);
        assertEquals(List.of(), monitor.check(List.of(verified), at(10, 41)));
    }

    @Test
    void closedBehavesAsResolved() {
        assertOneAlert(monitor.check(List.of(acked("INC1", Priority.P1, at(10, 3))), at(10, 24)),
                "INC1", SlaAlert.Type.WARNING);
        Incident closed = restore("INC1", IncidentStatus.CLOSED, Priority.P1, T0, at(10, 3), at(10, 31));
        Incident untracked = restore("INC2", IncidentStatus.CLOSED, Priority.P1, T0, at(10, 10), at(10, 31));

        List<SlaAlert> alerts = monitor.check(List.of(closed, untracked), at(10, 40));

        assertOneAlert(alerts, "INC1", SlaAlert.Type.BREACH);
        assertEquals(List.of(), monitor.check(List.of(closed), at(10, 41)));
    }

    // ---------- skipped incidents (D7, D8, D17, D20) ----------

    @Test
    void nullEntryIsSkippedAndTheRestChecked() {
        List<Incident> incidents = Collections.unmodifiableList(
                Arrays.asList(null, unacked("INC1", Priority.P1)));

        assertOneAlert(monitor.check(incidents, at(10, 4)), "INC1", SlaAlert.Type.WARNING);
        assertEquals(List.of(new SkippedIncident(0, null, "null entry")), monitor.lastSkipped());
    }

    @Test
    void missingPriorityIsSkipped() {
        assertEquals(List.of(), monitor.check(List.of(unacked("INC1", null)), at(10, 6)));
        assertEquals(List.of(new SkippedIncident(0, "INC1", "missing priority")), monitor.lastSkipped());
    }

    @Test
    void rejectedTimestampsAreSkippedWithReason() {
        // Acknowledged at 10:06 but checked at 10:05.
        assertEquals(List.of(), monitor.check(List.of(acked("INC1", Priority.P1, at(10, 6))), at(10, 5)));

        List<SkippedIncident> skipped = monitor.lastSkipped();
        assertEquals(1, skipped.size());
        assertEquals(0, skipped.get(0).index());
        assertEquals("INC1", skipped.get(0).incidentId());
        assertTrue(skipped.get(0).reason().contains("invalid SLA data"));
        assertTrue(skipped.get(0).reason().contains("acknowledgedAt"));
    }

    @Test
    void skippedIncidentKeepsItsMemory() {
        // P1 created 09:35:30, acknowledged on time at 09:38:
        // resolution warning 09:59:30, resolution due 10:05:30.
        LocalDateTime created = at(9, 35, 30);
        List<Incident> valid = List.of(
                restore("INC1", IncidentStatus.IN_PROGRESS, Priority.P1, created, at(9, 38), null));
        // Same incident, but acknowledged at 10:05:10 and checked at 10:05: rejected.
        List<Incident> invalid = List.of(
                restore("INC1", IncidentStatus.IN_PROGRESS, Priority.P1, created, at(10, 5, 10), null));

        assertOneAlert(monitor.check(valid, at(10, 4)), "INC1", SlaAlert.Type.WARNING);
        assertEquals(List.of(), monitor.check(invalid, at(10, 5)));
        assertEquals(1, monitor.lastSkipped().size());
        // Still AT_RISK (deadline reached, not passed) and still remembered: no new alert.
        assertEquals(List.of(), monitor.check(valid, at(10, 5, 30)));
        assertOneAlert(monitor.check(valid, at(10, 6)), "INC1", SlaAlert.Type.BREACH);
    }

    @Test
    void liveStatusWithResolvedAtIsSkippedAndMemoryKept() {
        assertOneAlert(monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4)),
                "INC1", SlaAlert.Type.WARNING);
        // IN_PROGRESS but with a resolution time: contradictory saved data.
        Incident mismatch = restore("INC1", IncidentStatus.IN_PROGRESS, Priority.P1, T0,
                at(10, 4, 10), at(10, 4, 20));

        assertEquals(List.of(), monitor.check(List.of(mismatch), at(10, 4, 30)));
        assertEquals(1, monitor.lastSkipped().size());
        assertTrue(monitor.lastSkipped().get(0).reason().contains("resolvedAt"));

        // Still remembered as AT_RISK, so no second warning.
        assertEquals(List.of(), monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4, 45)));
    }

    @Test
    void untrackedResolvedWithoutResolvedAtIsReported() {
        Incident mismatch = restore("INC9", IncidentStatus.RESOLVED, Priority.P1, T0, at(10, 1), null);

        assertEquals(List.of(), monitor.check(List.of(mismatch), at(10, 5)));

        List<SkippedIncident> skipped = monitor.lastSkipped();
        assertEquals(1, skipped.size());
        assertEquals(0, skipped.get(0).index());
        assertEquals("INC9", skipped.get(0).incidentId());
        assertTrue(skipped.get(0).reason().contains("resolvedAt"));
    }

    @Test
    void trackedCompletedWithMissingPriorityStaysTracked() {
        assertOneAlert(monitor.check(List.of(acked("INC1", Priority.P1, at(10, 3))), at(10, 24)),
                "INC1", SlaAlert.Type.WARNING);
        assertEquals(List.of(), monitor.check(
                List.of(resolved("INC1", null, at(10, 3), at(10, 31))), at(10, 32)));
        assertEquals(List.of(new SkippedIncident(0, "INC1", "missing priority")), monitor.lastSkipped());
        // Still tracked, so the valid final check reports the late resolution.
        assertOneAlert(monitor.check(List.of(resolved("INC1", Priority.P1, at(10, 3), at(10, 31))),
                at(10, 33)), "INC1", SlaAlert.Type.BREACH);
    }

    @Test
    void trackedCompletedWithRejectedDataIsRetried() {
        assertOneAlert(monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4)),
                "INC1", SlaAlert.Type.WARNING);
        // Resolution time 10:10 is after "now" 10:07: rejected, skipped, still tracked.
        Incident done = resolved("INC1", Priority.P1, at(10, 6), at(10, 10));
        assertEquals(List.of(), monitor.check(List.of(done), at(10, 7)));
        assertEquals(1, monitor.lastSkipped().size());
        assertTrue(monitor.lastSkipped().get(0).reason().contains("resolvedAt"));

        // Now valid: acknowledged late at 10:06, so one BREACH, then forgotten.
        assertOneAlert(monitor.check(List.of(done), at(10, 11)), "INC1", SlaAlert.Type.BREACH);
        assertEquals(List.of(), monitor.check(List.of(done), at(10, 12)));
    }

    // ---------- duplicate IDs (D9) ----------

    @Test
    void invalidFirstCopyDecides() {
        List<Incident> incidents = List.of(unacked("INC1", null), unacked("INC1", Priority.P1));

        assertEquals(List.of(), monitor.check(incidents, at(10, 4)));

        List<SkippedIncident> skipped = monitor.lastSkipped();
        assertEquals(2, skipped.size());
        assertEquals(new SkippedIncident(0, "INC1", "missing priority"), skipped.get(0));
        assertEquals(1, skipped.get(1).index());
        assertEquals("INC1", skipped.get(1).incidentId());
        assertTrue(skipped.get(1).reason().contains("duplicate"));
    }

    @Test
    void validFirstCopyGivesOneAlert() {
        List<Incident> incidents = List.of(unacked("INC1", Priority.P1), unacked("INC1", Priority.P1));

        assertOneAlert(monitor.check(incidents, at(10, 4)), "INC1", SlaAlert.Type.WARNING);
        assertEquals(1, monitor.lastSkipped().size());
        assertEquals(1, monitor.lastSkipped().get(0).index());
    }

    @Test
    void untrackedCompletedFirstCopyThenDuplicate() {
        List<Incident> incidents = List.of(
                resolved("INC1", Priority.P1, at(10, 1), at(10, 2)), unacked("INC1", Priority.P1));

        assertEquals(List.of(), monitor.check(incidents, at(10, 4)));

        List<SkippedIncident> skipped = monitor.lastSkipped();
        assertEquals(1, skipped.size());
        assertEquals(1, skipped.get(0).index());
        assertTrue(skipped.get(0).reason().contains("duplicate"));
    }

    // ---------- skip report ----------

    @Test
    void skipReportIsReplacedEachCheckAndUnmodifiable() {
        assertEquals(List.of(), monitor.lastSkipped());

        monitor.check(Arrays.asList(null, unacked("INC1", Priority.P1)), at(10, 1));
        assertEquals(1, monitor.lastSkipped().size());
        List<SkippedIncident> report = monitor.lastSkipped();
        assertThrows(UnsupportedOperationException.class,
                () -> report.add(new SkippedIncident(5, "X", "test")));

        monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 2));
        assertEquals(List.of(), monitor.lastSkipped());
    }

    // ---------- whole-check failures change nothing (D10, D4, atomicity) ----------

    @Test
    void nullListOrNowIsRejectedAndNothingChanges() {
        List<Incident> incidents = Arrays.asList(unacked("INC1", Priority.P1), null);
        assertOneAlert(monitor.check(incidents, at(10, 4)), "INC1", SlaAlert.Type.WARNING);
        List<SkippedIncident> before = monitor.lastSkipped();

        assertThrows(IllegalArgumentException.class, () -> monitor.check(null, at(10, 5)));
        assertThrows(IllegalArgumentException.class,
                () -> monitor.check(List.of(unacked("INC1", Priority.P1)), null));

        assertEquals(before, monitor.lastSkipped());
        // Memory still says AT_RISK: no second warning.
        assertEquals(List.of(), monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 5)));
    }

    @Test
    void failureMidwayCommitsNothing() {
        SwitchableCalculator stub = new SwitchableCalculator();
        SlaMonitor stubMonitor = new SlaMonitor(stub);
        stubMonitor.check(Arrays.asList((Incident) null), at(10, 1));
        List<SkippedIncident> before = stubMonitor.lastSkipped();
        List<Incident> incidents = List.of(unacked("INC1", Priority.P1), unacked("INC2", Priority.P2));

        stub.failForP2 = true;
        assertThrows(IllegalStateException.class, () -> stubMonitor.check(incidents, at(10, 4)));
        assertEquals(before, stubMonitor.lastSkipped());

        // INC1 was evaluated before the failure, but nothing was saved: it still warns.
        stub.failForP2 = false;
        assertOneAlert(stubMonitor.check(incidents, at(10, 4)), "INC1", SlaAlert.Type.WARNING);
    }

    @Test
    void calculatorReturningNullFailsTheCheck() {
        SlaMonitor stubMonitor = new SlaMonitor(fixedStatusCalculator(null));
        assertThrows(IllegalStateException.class,
                () -> stubMonitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4)));
    }

    @Test
    void brokenPolicyFailsTheCheckAndChangesNothing() {
        SwitchablePolicy policy = new SwitchablePolicy();
        SlaMonitor policyMonitor = new SlaMonitor(new DefaultSlaCalculator(policy));
        assertOneAlert(policyMonitor.check(Arrays.asList(unacked("INC1", Priority.P1), null), at(10, 4)),
                "INC1", SlaAlert.Type.WARNING);
        List<SkippedIncident> before = policyMonitor.lastSkipped();

        policy.brokenForP2 = true;
        List<Incident> incidents = List.of(unacked("INC1", Priority.P1), unacked("INC2", Priority.P2));
        assertThrows(IllegalStateException.class, () -> policyMonitor.check(incidents, at(10, 6)));
        assertEquals(before, policyMonitor.lastSkipped());

        // INC1 reached BREACHED in the failed check, but that was not saved.
        policy.brokenForP2 = false;
        assertOneAlert(policyMonitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 6)),
                "INC1", SlaAlert.Type.BREACH);
    }

    // ---------- interface injection and non-mutation ----------

    @Test
    void injectedCalculatorDecidesTheStatus() {
        SlaMonitor stubMonitor = new SlaMonitor(fixedStatusCalculator(SlaStatus.AT_RISK));
        // The real calculator would say ON_TRACK at creation time.
        assertOneAlert(stubMonitor.check(List.of(unacked("INC1", Priority.P1)), T0),
                "INC1", SlaAlert.Type.WARNING);
    }

    @Test
    void monitorDoesNotChangeTheListOrIncidents() {
        Incident incident = acked("INC1", Priority.P1, at(10, 3));
        Incident other = unacked("INC2", Priority.P2);
        List<Incident> input = new ArrayList<>(List.of(incident, other));
        List<Incident> before = new ArrayList<>(input);

        monitor.check(input, at(10, 24));

        assertEquals(before, input);
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertEquals(Priority.P1, incident.getPriority());
        assertEquals(T0, incident.getCreatedAt());
        assertEquals(at(10, 3), incident.getAcknowledgedAt());
        assertNull(incident.getResolvedAt());
        assertEquals(IncidentStatus.ASSIGNED, other.getStatus());
        assertNull(other.getAcknowledgedAt());

        // An immutable List.of input also works.
        assertOneAlert(monitor.check(List.of(incident), at(10, 31)), "INC1", SlaAlert.Type.BREACH);
    }

    @Test
    void returnedAlertListIsUnmodifiable() {
        List<SlaAlert> alerts = monitor.check(List.of(unacked("INC1", Priority.P1)), at(10, 4));
        assertThrows(UnsupportedOperationException.class, () -> alerts.add(alerts.get(0)));
    }

    @Test
    void earlierNowIsAcceptedButMayRepeatAlerts() {
        // Documented limitation (U3): callers must pass non-decreasing times.
        List<Incident> incidents = List.of(unacked("INC1", Priority.P1));
        assertOneAlert(monitor.check(incidents, at(10, 6)), "INC1", SlaAlert.Type.BREACH);
        assertOneAlert(monitor.check(incidents, at(10, 4)), "INC1", SlaAlert.Type.WARNING);
        assertOneAlert(monitor.check(incidents, at(10, 6)), "INC1", SlaAlert.Type.BREACH);
    }

    // ---------- SlaAlert validation ----------

    @Test
    void alertRejectsMissingFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new SlaAlert(null, SlaAlert.Type.WARNING, "message"));
        assertThrows(IllegalArgumentException.class,
                () -> new SlaAlert("  ", SlaAlert.Type.WARNING, "message"));
        assertThrows(IllegalArgumentException.class,
                () -> new SlaAlert("INC1", null, "message"));
        assertThrows(IllegalArgumentException.class,
                () -> new SlaAlert("INC1", SlaAlert.Type.BREACH, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SlaAlert("INC1", SlaAlert.Type.BREACH, " "));
    }
}
