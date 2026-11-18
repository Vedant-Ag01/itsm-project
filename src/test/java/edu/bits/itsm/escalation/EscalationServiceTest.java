package edu.bits.itsm.escalation;

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
import edu.bits.itsm.escalation.EscalationService.SkippedIncident;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static edu.bits.itsm.escalation.EscalationLevel.IT_MANAGER;
import static edu.bits.itsm.escalation.EscalationLevel.SYSTEM_ADMIN;
import static edu.bits.itsm.escalation.EscalationLevel.TEAM_LEAD;
import static edu.bits.itsm.escalation.EscalationLevel.TECHNICIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Expected results are worked out by hand. Incidents are created at 10:00 on 2026-11-12
// unless a test says otherwise. Default policy: P1 steps due at 10:10 / 10:20 / 10:30,
// P2 at 10:30 / 11:00 / 11:30, P3 at 12:00 / 14:00 / 16:00, P4 never.
class EscalationServiceTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 12, 10, 0);
    private static final Duration NS = Duration.ofNanos(1);

    private static final Employee REPORTER = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private static final ItService SERVICE = new ItService("Payment application", BusinessCriticality.HIGH);
    private static final Technician RAVI = new Technician("T1", "Ravi", "ravi@bits.edu");

    private final FakeStore store = new FakeStore();
    private final EscalationService service =
            new EscalationService(new TimeBasedEscalationPolicy(), store);

    // ---------- helpers ----------

    private static LocalDateTime at(int hour, int minute) {
        return LocalDateTime.of(2026, 11, 12, hour, minute);
    }

    // Test-only use of Incident.restore: it rebuilds a saved snapshot with fixed
    // timestamps, which the normal lifecycle methods cannot do (they read the clock).
    private static Incident restore(String id, IncidentStatus status, Priority priority,
                                    Technician technician, LocalDateTime createdAt,
                                    LocalDateTime acknowledgedAt, LocalDateTime resolvedAt) {
        return Incident.restore(id, REPORTER, "Payment down", "Payments are failing",
                SERVICE, Impact.HIGH, Urgency.HIGH, 50, createdAt, status,
                IncidentCategory.APPLICATION, priority, technician,
                acknowledgedAt, resolvedAt, null, resolvedAt == null ? null : "Restarted service",
                null, new ArrayList<>(), null);
    }

    /** Assigned to Ravi, not acknowledged, created at 10:00. */
    private static Incident assigned(String id, Priority priority) {
        return restore(id, IncidentStatus.ASSIGNED, priority, RAVI, T0, null, null);
    }

    private List<EscalationAction> scan(LocalDateTime now, Incident... incidents) {
        return service.findEscalations(List.of(incidents), now);
    }

    private static void assertStep(EscalationAction action, String id,
                                   EscalationLevel from, EscalationLevel to) {
        assertEquals(id, action.incidentId());
        assertEquals(from, action.previousLevel());
        assertEquals(to, action.newLevel());
    }

    /** An all-or-nothing level store the tests can inspect and break on purpose. */
    private static final class FakeStore implements EscalationLevelStore {
        final Map<String, EscalationLevel> levels = new HashMap<>();
        String returnNullFor;
        String failReadFor;
        boolean failSaves;
        int saveCalls;

        @Override
        public Optional<EscalationLevel> findLevel(String incidentId) {
            if (incidentId.equals(failReadFor)) {
                throw new IllegalStateException("database unavailable");
            }
            if (incidentId.equals(returnNullFor)) {
                return null;
            }
            return Optional.ofNullable(levels.get(incidentId));
        }

        @Override
        public void saveLevels(Map<String, EscalationLevel> newLevels) {
            saveCalls++;
            if (failSaves) {
                throw new IllegalStateException("db write failed"); // nothing written
            }
            levels.putAll(newLevels);
        }
    }

    // ---------- 1. basic results ----------

    @Test
    void emptyListGivesNoEscalations() {
        assertTrue(service.findEscalations(List.of(), T0).isEmpty());
        assertTrue(service.lastSkipped().isEmpty());
    }

    @Test
    void noEscalationJustBeforeTheFirstThreshold() {
        Incident p1 = assigned("INC1", Priority.P1);

        assertTrue(scan(at(10, 10).minus(NS), p1).isEmpty());
        assertTrue(store.levels.isEmpty());
    }

    @Test
    void zeroElapsedTimeIsValid() {
        assertTrue(scan(T0, assigned("INC1", Priority.P1)).isEmpty());
        assertTrue(service.lastSkipped().isEmpty());
    }

    @Test
    void firstEscalationAtExactlyTenMinutesHasAllDetails() {
        List<EscalationAction> actions = scan(at(10, 10), assigned("INC1", Priority.P1));

        assertEquals(1, actions.size());
        EscalationAction action = actions.get(0);
        assertStep(action, "INC1", TECHNICIAN, TEAM_LEAD);
        assertEquals(Priority.P1, action.priority());
        assertEquals(Duration.ofMinutes(10), action.timeUnacknowledged());
        assertEquals(at(10, 10), action.checkedAt());
        assertTrue(action.reason().contains("INC1"));
        assertTrue(action.reason().contains("TECHNICIAN"));
        assertTrue(action.reason().contains("TEAM_LEAD"));
        assertEquals(TEAM_LEAD, store.levels.get("INC1"));
    }

    @Test
    void firstSeenAfterTheThresholdEscalatesImmediately() {
        // Discovery does not start a fresh 10-minute wait: the clock runs from creation.
        List<EscalationAction> actions = scan(at(10, 11), assigned("INC1", Priority.P1));

        assertEquals(1, actions.size());
        assertStep(actions.get(0), "INC1", TECHNICIAN, TEAM_LEAD);
    }

    // ---------- 2. mixed priorities and order ----------

    @Test
    void eachPriorityUsesItsOwnIntervalAndInputOrderIsKept() {
        Incident p3 = assigned("P3-A", Priority.P3);
        Incident p1 = assigned("P1-A", Priority.P1);
        Incident closed = restore("DONE", IncidentStatus.CLOSED, Priority.P1, RAVI, T0, null, at(10, 5));
        Incident p4 = assigned("P4-A", Priority.P4);
        Incident p2 = assigned("P2-A", Priority.P2);

        List<EscalationAction> actions = scan(at(10, 30), p3, p1, closed, p4, p2);

        assertEquals(2, actions.size());
        assertStep(actions.get(0), "P1-A", TECHNICIAN, TEAM_LEAD); // 30 min: still one step
        assertStep(actions.get(1), "P2-A", TECHNICIAN, TEAM_LEAD); // exactly 30 min
    }

    @Test
    void p2AndP3StepThroughAllLevelsViaTheService() {
        Incident p2 = assigned("P2", Priority.P2);
        Incident p3 = assigned("P3", Priority.P3);

        assertStep(scan(at(10, 30), p2, p3).get(0), "P2", TECHNICIAN, TEAM_LEAD);
        assertTrue(scan(at(11, 0).minus(NS), p2, p3).isEmpty());
        assertStep(scan(at(11, 0), p2, p3).get(0), "P2", TEAM_LEAD, SYSTEM_ADMIN);
        assertStep(scan(at(11, 30), p2, p3).get(0), "P2", SYSTEM_ADMIN, IT_MANAGER);

        assertTrue(scan(at(12, 0).minus(NS), p2, p3).isEmpty());
        assertStep(scan(at(12, 0), p2, p3).get(0), "P3", TECHNICIAN, TEAM_LEAD);
        assertStep(scan(at(14, 0), p2, p3).get(0), "P3", TEAM_LEAD, SYSTEM_ADMIN);
        assertTrue(scan(at(16, 0).minus(NS), p2, p3).isEmpty());
        assertStep(scan(at(16, 0), p2, p3).get(0), "P3", SYSTEM_ADMIN, IT_MANAGER);
        assertTrue(scan(at(23, 0), p2, p3).isEmpty());
    }

    @Test
    void hugeElapsedTimeGivesOneStep() {
        Incident ancient = restore("OLD", IncidentStatus.ASSIGNED, Priority.P1, RAVI,
                LocalDateTime.MIN, null, null);

        List<EscalationAction> actions = scan(LocalDateTime.MAX, ancient);

        assertEquals(1, actions.size());
        assertStep(actions.get(0), "OLD", TECHNICIAN, TEAM_LEAD);
    }

    @Test
    void p4NeverEscalatesEvenAfterDays() {
        Incident p4 = assigned("INC4", Priority.P4);

        assertTrue(scan(at(10, 0).plusDays(30), p4).isEmpty());
        assertTrue(store.levels.isEmpty());
    }

    // ---------- 3. eligibility ----------

    @Test
    void unassignedClassifiedIncidentEscalates() {
        Incident unassigned = restore("INC1", IncidentStatus.CLASSIFIED, Priority.P1, null, T0, null, null);

        List<EscalationAction> actions = scan(at(10, 10), unassigned);

        assertEquals(1, actions.size());
        assertStep(actions.get(0), "INC1", TECHNICIAN, TEAM_LEAD);
    }

    @Test
    void acknowledgedAndLaterStatesAreSkippedQuietly() {
        Incident acknowledged = restore("ACK", IncidentStatus.ACKNOWLEDGED, Priority.P1, RAVI,
                T0, at(10, 25), null);
        Incident inProgress = restore("WIP", IncidentStatus.IN_PROGRESS, Priority.P1, RAVI,
                T0, at(10, 2), null);
        // Terminal snapshots with a missing acknowledgement must not escalate either.
        Incident resolved = restore("RES", IncidentStatus.RESOLVED, Priority.P1, RAVI, T0, null, at(10, 5));
        Incident verified = restore("VER", IncidentStatus.VERIFIED, Priority.P1, RAVI, T0, null, null);
        Incident closed = restore("CLO", IncidentStatus.CLOSED, Priority.P1, RAVI, T0, null, null);
        Incident fresh = restore("NEW", IncidentStatus.NEW, null, null, T0, null, null);

        assertTrue(scan(at(11, 0), acknowledged, inProgress, resolved, verified, closed, fresh).isEmpty());
        assertTrue(service.lastSkipped().isEmpty());
        assertTrue(store.levels.isEmpty());
    }

    @Test
    void lateAcknowledgementStopsFurtherEscalation() {
        assertEquals(1, scan(at(10, 10), assigned("INC1", Priority.P1)).size());

        Incident acknowledgedLate = restore("INC1", IncidentStatus.ACKNOWLEDGED, Priority.P1, RAVI,
                T0, at(10, 12), null);

        assertTrue(scan(at(10, 20), acknowledgedLate).isEmpty());
        assertTrue(scan(at(10, 40), acknowledgedLate).isEmpty());
        assertEquals(TEAM_LEAD, store.levels.get("INC1")); // history kept, not demoted
    }

    // ---------- 4. current level comes from the store ----------

    @Test
    void currentLevelIsReadFromTheStore() {
        store.levels.put("INC1", TEAM_LEAD);

        assertTrue(scan(at(10, 15), assigned("INC1", Priority.P1)).isEmpty());
        List<EscalationAction> actions = scan(at(10, 20), assigned("INC1", Priority.P1));
        assertStep(actions.get(0), "INC1", TEAM_LEAD, SYSTEM_ADMIN);
    }

    @Test
    void incidentAlreadyAtTheTopIsNotEscalated() {
        store.levels.put("INC1", IT_MANAGER);

        assertTrue(scan(at(18, 0), assigned("INC1", Priority.P1)).isEmpty());
    }

    @Test
    void storeReturningNullFailsTheScanAndSavesNothing() {
        store.returnNullFor = "SECOND";

        // FIRST is due and evaluated before the failure, but must not be saved.
        assertThrows(IllegalStateException.class,
                () -> scan(at(10, 10), assigned("FIRST", Priority.P1), assigned("SECOND", Priority.P1)));
        assertTrue(store.levels.isEmpty());
        assertEquals(0, store.saveCalls);
    }

    @Test
    void storeFailureIsNotTreatedAsTechnician() {
        store.levels.put("SECOND", SYSTEM_ADMIN);
        store.failReadFor = "SECOND";

        assertThrows(IllegalStateException.class,
                () -> scan(at(10, 30), assigned("FIRST", Priority.P1), assigned("SECOND", Priority.P1)));
        assertEquals(Map.of("SECOND", SYSTEM_ADMIN), store.levels);
    }

    @Test
    void allNewLevelsAreSavedInOneCallAndOnlyWhenSomethingChanged() {
        scan(at(10, 5), assigned("A", Priority.P1));
        assertEquals(0, store.saveCalls);

        scan(at(10, 10), assigned("A", Priority.P1), assigned("B", Priority.P1));
        assertEquals(1, store.saveCalls);
        assertEquals(Map.of("A", TEAM_LEAD, "B", TEAM_LEAD), store.levels);
    }

    @Test
    void failedSaveReturnsNothingAndTheNextScanFindsEverythingAgain() {
        // Regression for review finding D1: a save failure must not lose escalations.
        scan(at(10, 0), restore("NOPRI", IncidentStatus.ASSIGNED, null, RAVI, T0, null, null));
        List<SkippedIncident> before = service.lastSkipped();
        List<Incident> incidents = List.of(assigned("A", Priority.P1), assigned("B", Priority.P1));
        store.failSaves = true;

        assertThrows(IllegalStateException.class, () -> service.findEscalations(incidents, at(10, 10)));
        assertTrue(store.levels.isEmpty());
        assertSame(before, service.lastSkipped()); // unchanged by the failed scan

        store.failSaves = false;
        List<EscalationAction> retry = service.findEscalations(incidents, at(10, 10));
        assertEquals(2, retry.size());
        assertStep(retry.get(0), "A", TECHNICIAN, TEAM_LEAD);
        assertStep(retry.get(1), "B", TECHNICIAN, TEAM_LEAD);
    }

    // ---------- 5-7. repeated and delayed scans ----------

    @Test
    void repeatedScansFollowCumulativeThresholds() {
        Incident p1 = assigned("INC1", Priority.P1);

        assertStep(scan(at(10, 10), p1).get(0), "INC1", TECHNICIAN, TEAM_LEAD);
        assertTrue(scan(at(10, 10), p1).isEmpty()); // same scan again: already done
        assertTrue(scan(at(10, 11), p1).isEmpty());
        assertTrue(scan(at(10, 20).minus(NS), p1).isEmpty());
        assertStep(scan(at(10, 20), p1).get(0), "INC1", TEAM_LEAD, SYSTEM_ADMIN);
        assertTrue(scan(at(10, 29), p1).isEmpty());
        assertStep(scan(at(10, 30), p1).get(0), "INC1", SYSTEM_ADMIN, IT_MANAGER);
        assertTrue(scan(at(12, 0), p1).isEmpty());
    }

    @Test
    void lateFirstEscalationDoesNotPostponeTheSecond() {
        Incident p1 = assigned("INC1", Priority.P1);

        assertStep(scan(at(10, 12), p1).get(0), "INC1", TECHNICIAN, TEAM_LEAD);
        assertTrue(scan(at(10, 20).minus(NS), p1).isEmpty());
        assertStep(scan(at(10, 20), p1).get(0), "INC1", TEAM_LEAD, SYSTEM_ADMIN); // not 10:22
    }

    @Test
    void missedChecksCatchUpOneLevelPerScan() {
        Incident p1 = assigned("INC1", Priority.P1);

        assertStep(scan(at(10, 35), p1).get(0), "INC1", TECHNICIAN, TEAM_LEAD);
        assertStep(scan(at(10, 36), p1).get(0), "INC1", TEAM_LEAD, SYSTEM_ADMIN);
        assertStep(scan(at(10, 37), p1).get(0), "INC1", SYSTEM_ADMIN, IT_MANAGER);
        assertTrue(scan(at(10, 38), p1).isEmpty());
    }

    @Test
    void acknowledgementBetweenCatchUpScansStopsIt() {
        assertEquals(1, scan(at(10, 35), assigned("INC1", Priority.P1)).size());

        Incident acknowledged = restore("INC1", IncidentStatus.ACKNOWLEDGED, Priority.P1, RAVI,
                T0, at(10, 35), null);

        assertTrue(scan(at(10, 36), acknowledged).isEmpty());
    }

    @Test
    void severalDueIncidentsEachGetOneStep() {
        List<EscalationAction> actions = scan(at(10, 35),
                assigned("A", Priority.P1), assigned("B", Priority.P1), assigned("C", Priority.P2));

        assertEquals(3, actions.size());
        assertStep(actions.get(0), "A", TECHNICIAN, TEAM_LEAD);
        assertStep(actions.get(1), "B", TECHNICIAN, TEAM_LEAD);
        assertStep(actions.get(2), "C", TECHNICIAN, TEAM_LEAD);
    }

    // ---------- 8. failures and restart ----------

    @Test
    void failedScanSavesNothingSoTheNextScanFindsTheSameEscalation() {
        EscalationPolicy brokenForB = (priority, current, elapsed) -> priority == Priority.P2
                ? null
                : new TimeBasedEscalationPolicy().nextLevel(priority, current, elapsed);
        EscalationService fragile = new EscalationService(brokenForB, store);
        List<Incident> incidents = List.of(assigned("A", Priority.P1), assigned("B", Priority.P2));

        assertThrows(IllegalStateException.class, () -> fragile.findEscalations(incidents, at(10, 30)));
        assertTrue(store.levels.isEmpty()); // A's step was not saved

        List<EscalationAction> retry = service.findEscalations(incidents, at(10, 30));
        assertEquals(2, retry.size());
        assertStep(retry.get(0), "A", TECHNICIAN, TEAM_LEAD);
    }

    @Test
    void newServiceWithTheSameStoreContinuesWhereItStopped() {
        Incident p1 = assigned("INC1", Priority.P1);
        scan(at(10, 10), p1);

        EscalationService restarted = new EscalationService(new TimeBasedEscalationPolicy(), store);

        assertTrue(restarted.findEscalations(List.of(p1), at(10, 15)).isEmpty());
        assertStep(restarted.findEscalations(List.of(p1), at(10, 20)).get(0), "INC1", TEAM_LEAD, SYSTEM_ADMIN);
    }

    @Test
    void defaultInMemoryStoreForgetsLevelsWhenServiceIsRecreated() {
        // Documented limitation: without a durable store a restart starts again at TECHNICIAN.
        Incident p1 = assigned("INC1", Priority.P1);
        EscalationService first = new EscalationService(new TimeBasedEscalationPolicy());
        assertEquals(1, first.findEscalations(List.of(p1), at(10, 10)).size());
        assertTrue(first.findEscalations(List.of(p1), at(10, 15)).isEmpty());

        EscalationService second = new EscalationService(new TimeBasedEscalationPolicy());
        assertStep(second.findEscalations(List.of(p1), at(10, 15)).get(0), "INC1", TECHNICIAN, TEAM_LEAD);
    }

    // ---------- 9. priority changes, bad data, duplicates ----------

    @Test
    void raisedPriorityUsesTheNewIntervalFromCreation() {
        Incident incident = assigned("INC1", Priority.P2);
        assertTrue(scan(at(10, 15), incident).isEmpty());

        incident.setPriority(Priority.P1);

        List<EscalationAction> actions = scan(at(10, 15), incident);
        assertStep(actions.get(0), "INC1", TECHNICIAN, TEAM_LEAD);
        assertEquals(Priority.P1, actions.get(0).priority());
    }

    @Test
    void loweredPriorityNeverDemotes() {
        Incident incident = assigned("INC1", Priority.P1);
        scan(at(10, 10), incident);

        incident.setPriority(Priority.P3); // TEAM_LEAD -> SYSTEM_ADMIN now due at 14:00
        assertTrue(scan(at(10, 25), incident).isEmpty());
        assertEquals(TEAM_LEAD, store.levels.get("INC1"));
        assertStep(scan(at(14, 0), incident).get(0), "INC1", TEAM_LEAD, SYSTEM_ADMIN);

        incident.setPriority(Priority.P4); // stops further escalation
        assertTrue(scan(at(23, 0), incident).isEmpty());
        assertEquals(SYSTEM_ADMIN, store.levels.get("INC1"));
    }

    @Test
    void badIncidentsAreSkippedAndReportedWhileOthersContinue() {
        Incident noPriority = restore("NOPRI", IncidentStatus.ASSIGNED, null, RAVI, T0, null, null);
        Incident ackButAssigned = restore("ACKSET", IncidentStatus.ASSIGNED, Priority.P1, RAVI,
                T0, at(10, 1), null);
        Incident resolvedButClassified = restore("RESSET", IncidentStatus.CLASSIFIED, Priority.P1, null,
                T0, null, at(10, 2));
        Incident future = restore("FUTURE", IncidentStatus.ASSIGNED, Priority.P1, RAVI,
                at(11, 0), null, null);
        Incident good = assigned("GOOD", Priority.P1);

        List<EscalationAction> actions = service.findEscalations(
                Arrays.asList(noPriority, null, ackButAssigned, resolvedButClassified, future, good),
                at(10, 10));

        assertEquals(1, actions.size());
        assertStep(actions.get(0), "GOOD", TECHNICIAN, TEAM_LEAD);

        List<SkippedIncident> skipped = service.lastSkipped();
        assertEquals(5, skipped.size());
        assertEquals(new SkippedIncident(0, "NOPRI", "missing priority"), skipped.get(0));
        assertEquals(1, skipped.get(1).index());
        assertNull(skipped.get(1).incidentId());
        assertTrue(skipped.get(2).reason().contains("acknowledgedAt"));
        assertTrue(skipped.get(3).reason().contains("resolvedAt"));
        assertEquals("FUTURE", skipped.get(4).incidentId());
        assertTrue(skipped.get(4).reason().contains("after the scan time"));
        assertEquals(Map.of("GOOD", TEAM_LEAD), store.levels);
    }

    @Test
    void duplicateIdsAreSkippedAndTheFirstCopyDecides() {
        Incident first = assigned("INC1", Priority.P1);
        Incident copy = assigned("INC1", Priority.P1);

        List<EscalationAction> actions = scan(at(10, 10), first, copy);
        assertEquals(1, actions.size());
        assertEquals(List.of(new SkippedIncident(1, "INC1", EscalationService.DUPLICATE_REASON)),
                service.lastSkipped());

        // An ignored first copy (acknowledged) still decides.
        Incident acknowledged = restore("INC2", IncidentStatus.ACKNOWLEDGED, Priority.P1, RAVI,
                T0, at(10, 1), null);
        assertTrue(scan(at(10, 10), acknowledged, assigned("INC2", Priority.P1)).isEmpty());
        assertEquals(1, service.lastSkipped().size());
    }

    @Test
    void duplicateOfASkippedFirstCopyIsStillADuplicate() {
        Incident badFirst = restore("INC1", IncidentStatus.ASSIGNED, null, RAVI, T0, null, null);
        Incident goodSecond = assigned("INC1", Priority.P1);

        assertTrue(scan(at(10, 10), badFirst, goodSecond).isEmpty());
        assertEquals(List.of(new SkippedIncident(0, "INC1", "missing priority"),
                        new SkippedIncident(1, "INC1", EscalationService.DUPLICATE_REASON)),
                service.lastSkipped());
    }

    @Test
    void lastSkippedIsReplacedByEachScan() {
        scan(at(10, 10), restore("NOPRI", IncidentStatus.ASSIGNED, null, RAVI, T0, null, null));
        assertEquals(1, service.lastSkipped().size());

        scan(at(10, 10), assigned("INC1", Priority.P1));
        assertTrue(service.lastSkipped().isEmpty());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> service.findEscalations(null, T0));
        assertThrows(IllegalArgumentException.class, () -> service.findEscalations(List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> new EscalationService(null));
        assertThrows(IllegalArgumentException.class,
                () -> new EscalationService(new TimeBasedEscalationPolicy(), null));
        assertThrows(IllegalArgumentException.class,
                () -> new EscalationService(null, new InMemoryEscalationLevelStore()));
    }

    // ---------- 10. delegation, policy contract, no mutation ----------

    @Test
    void serviceDelegatesToTheInjectedPolicy() {
        List<Object[]> calls = new ArrayList<>();
        EscalationPolicy alwaysUp = (priority, current, elapsed) -> {
            calls.add(new Object[] {priority, current, elapsed});
            return TEAM_LEAD;
        };
        EscalationService custom = new EscalationService(alwaysUp, store);

        List<EscalationAction> actions = custom.findEscalations(
                List.of(assigned("INC1", Priority.P3)), at(10, 1));

        assertEquals(1, calls.size());
        assertEquals(Priority.P3, calls.get(0)[0]);
        assertEquals(TECHNICIAN, calls.get(0)[1]);
        assertEquals(Duration.ofMinutes(1), calls.get(0)[2]);
        assertStep(actions.get(0), "INC1", TECHNICIAN, TEAM_LEAD);
    }

    @Test
    void invalidPolicyResultsAreRejectedAndNothingIsSaved() {
        List<Incident> incidents = List.of(assigned("OK", Priority.P1), assigned("BAD", Priority.P1));
        store.levels.put("BAD", SYSTEM_ADMIN);

        EscalationPolicy returnsNull = (p, current, e) -> current == SYSTEM_ADMIN ? null : TEAM_LEAD;
        EscalationPolicy demotes = (p, current, e) -> TEAM_LEAD; // SYSTEM_ADMIN -> TEAM_LEAD
        EscalationPolicy jumps = (p, current, e) -> current == SYSTEM_ADMIN ? SYSTEM_ADMIN : IT_MANAGER;

        for (EscalationPolicy broken : List.of(returnsNull, demotes, jumps)) {
            EscalationService bad = new EscalationService(broken, store);
            assertThrows(IllegalStateException.class, () -> bad.findEscalations(incidents, at(10, 10)));
            assertEquals(Map.of("BAD", SYSTEM_ADMIN), store.levels);
        }
    }

    @Test
    void scanDoesNotChangeTheIncidentsOrTheList() {
        Incident incident = assigned("INC1", Priority.P1);
        List<Incident> incidents = List.of(incident); // unmodifiable on purpose

        List<EscalationAction> actions = service.findEscalations(incidents, at(10, 35));

        assertEquals(1, actions.size());
        assertSame(incident, incidents.get(0));
        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus());
        assertEquals(Priority.P1, incident.getPriority());
        assertSame(RAVI, incident.getAssignedTechnician());
        assertEquals(T0, incident.getCreatedAt());
        assertNull(incident.getAcknowledgedAt());
        assertNull(incident.getResolvedAt());
        assertThrows(UnsupportedOperationException.class, () -> actions.add(actions.get(0)));
        assertThrows(UnsupportedOperationException.class, () -> service.lastSkipped().clear());
    }

    @Test
    void actionRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new EscalationAction(null, Priority.P1,
                TECHNICIAN, TEAM_LEAD, Duration.ZERO, T0, "r"));
        assertThrows(IllegalArgumentException.class, () -> new EscalationAction("A", Priority.P1,
                TECHNICIAN, SYSTEM_ADMIN, Duration.ZERO, T0, "r"));
        assertThrows(IllegalArgumentException.class, () -> new EscalationAction("A", Priority.P1,
                TEAM_LEAD, TEAM_LEAD, Duration.ZERO, T0, "r"));
    }
}
