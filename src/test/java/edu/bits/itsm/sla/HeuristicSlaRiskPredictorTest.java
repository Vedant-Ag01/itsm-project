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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeuristicSlaRiskPredictorTest {
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 11, 19, 10, 0);
    private static final Employee REPORTER = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private static final ItService SERVICE = new ItService("Payment application", BusinessCriticality.HIGH);
    private static final Technician TECHNICIAN = new Technician("T1", "Ravi", "ravi@bits.edu");

    private final SlaCalculator calculator = new DefaultSlaCalculator(new DefaultSlaPolicy());
    private final SlaRiskPredictor predictor = new HeuristicSlaRiskPredictor(calculator);

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"CLASSIFIED", "ASSIGNED"})
    void responsePressureRisesEvenWithAnAssignment(IncidentStatus status) {
        Incident incident = restore(status, Priority.P2, null, null);
        assertEquals(22, predictor.assess(incident, 3, CREATED).score());

        SlaRisk result = predictor.assess(incident, 3, CREATED.plusMinutes(12));
        assertEquals(new SlaRisk(RiskLevel.HIGH, 78,
                "The response deadline is due on 2026-11-19 at 10:15 and the incident is still unacknowledged."),
                result);
        assertEquals(result, predictor.assess(incident, 3, CREATED.plusMinutes(12)));
        assertEquals(status, incident.getStatus());
        assertNull(incident.getAcknowledgedAt());
    }

    @Test
    void acknowledgementSwitchesTheAssessmentToResolution() {
        Incident incident = restore(IncidentStatus.IN_PROGRESS, Priority.P2, CREATED.plusMinutes(10), null);

        assertEquals(new SlaRisk(RiskLevel.LOW, 19, "The resolution deadline is due on 2026-11-19 at 12:00."),
                predictor.assess(incident, 3, CREATED.plusMinutes(12)));
    }

    @Test
    void lateAcknowledgementLeavesOnlyTheOriginalResolutionClockOpen() {
        LocalDateTime acknowledgedAt = CREATED.plusMinutes(20);
        LocalDateTime now = CREATED.plusMinutes(21);
        Incident incident = restore(IncidentStatus.IN_PROGRESS, Priority.P2, acknowledgedAt, null);
        assertEquals(SlaStatus.BREACHED, calculator.evaluate(CREATED, Priority.P2, acknowledgedAt, null, now));

        SlaRisk early = predictor.assess(incident, 3, now);
        assertEquals(RiskLevel.LOW, early.level());
        assertEquals(24, early.score());
        assertTrue(early.reason().contains("resolution deadline"));
        assertEquals(68, predictor.assess(incident, 3, CREATED.plusMinutes(96)).score());
    }

    @ParameterizedTest
    @CsvSource({"ASSIGNED, 15, response, 85, 86", "IN_PROGRESS, 120, resolution, 75, 76"})
    void bothDeadlineTypesBecomeOverdueStrictlyAfterEquality(IncidentStatus status, long targetMinutes,
                                                              String kind, int beforeScore, int dueScore) {
        boolean response = status == IncidentStatus.ASSIGNED;
        Incident incident = restore(status, Priority.P2, response ? null : CREATED.plusMinutes(2), null);
        LocalDateTime deadline = CREATED.plusMinutes(targetMinutes);

        SlaRisk before = predictor.assess(incident, 0, deadline.minusNanos(1));
        SlaRisk due = predictor.assess(incident, 0, deadline);
        SlaRisk overdue = predictor.assess(incident, 0, deadline.plusNanos(1));

        assertEquals(beforeScore, before.score());
        assertTrue(before.reason().startsWith("The " + kind + " deadline is due on "));
        assertEquals(dueScore, due.score());
        assertTrue(due.reason().startsWith("The " + kind + " deadline is due now"));
        assertEquals(new SlaRisk(RiskLevel.HIGH, 100, "The " + kind + " deadline is already overdue."), overdue);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"RESOLVED", "VERIFIED", "CLOSED"})
    void completedIncidentsAreLowDespiteHistoricalBreaches(IncidentStatus status) {
        LocalDateTime acknowledgedAt = CREATED.plusMinutes(6);
        LocalDateTime resolvedAt = CREATED.plusMinutes(45);
        LocalDateTime now = CREATED.plusDays(30);
        Incident incident = restore(status, Priority.P1, acknowledgedAt, resolvedAt);
        assertEquals(SlaStatus.BREACHED, calculator.evaluate(CREATED, Priority.P1, acknowledgedAt, resolvedAt, now));

        // This calculator has no usable targets, so a deadline lookup would fail the assessment.
        SlaRiskPredictor noDeadlines = predictorWithTargets(Duration.ZERO, Duration.ZERO);
        assertEquals(new SlaRisk(RiskLevel.LOW, 0, "The incident is resolved and has no outstanding SLA deadline."),
                noDeadlines.assess(incident, Integer.MAX_VALUE, now));
    }

    @ParameterizedTest
    @CsvSource({"P1, 45", "P2, 41", "P3, 38", "P4, 35"})
    void priorityChangesTheScoreAtTheSameElapsedFraction(Priority priority, int expectedScore) {
        SlaRiskPredictor custom = predictorWithTargets(Duration.ofSeconds(70), Duration.ofSeconds(70));
        Incident incident = restore(IncidentStatus.IN_PROGRESS, priority, CREATED, null);

        assertEquals(expectedScore, custom.assess(incident, 0, CREATED.plusSeconds(35)).score());
    }

    @Test
    void missingAcknowledgementAddsPointsEvenWithEqualDeadlines() {
        SlaRiskPredictor custom = predictorWithTargets(Duration.ofSeconds(70), Duration.ofSeconds(70));
        LocalDateTime now = CREATED.plusSeconds(35);
        Incident assigned = restore(IncidentStatus.ASSIGNED, Priority.P4, null, null);
        Incident acknowledged = restore(IncidentStatus.IN_PROGRESS, Priority.P4, CREATED, null);

        assertEquals(45, custom.assess(assigned, 0, now).score());
        assertEquals(35, custom.assess(acknowledged, 0, now).score());
    }

    @ParameterizedTest
    @CsvSource({"0, LOW", "39, LOW", "40, MEDIUM", "69, MEDIUM", "70, HIGH"})
    void scoreCutoffsAreExact(int seconds, RiskLevel expectedLevel) {
        SlaRiskPredictor custom = predictorWithTargets(Duration.ofSeconds(70), Duration.ofSeconds(70));
        Incident incident = restore(IncidentStatus.IN_PROGRESS, Priority.P4, CREATED, null);

        SlaRisk risk = custom.assess(incident, 0, CREATED.plusSeconds(seconds));
        assertEquals(seconds, risk.score());
        assertEquals(expectedLevel, risk.level());
    }

    @Test
    void loadRaisesRiskAndCapsSafelyEvenAtTheLargestInteger() {
        Incident incident = restore(IncidentStatus.ASSIGNED, Priority.P1, null, null);
        LocalDateTime now = CREATED.plusMinutes(5);

        assertEquals(90, predictor.assess(incident, 0, now).score());
        assertEquals(94, predictor.assess(incident, 2, now).score());
        assertEquals(100, predictor.assess(incident, 5, now).score());
        assertEquals(predictor.assess(incident, 5, now), predictor.assess(incident, Integer.MAX_VALUE, now));
    }

    @Test
    void customCalculatorCanMakeResolutionTheMoreUrgentOpenTarget() {
        SlaRiskPredictor custom = predictorWithTargets(Duration.ofSeconds(90), Duration.ofSeconds(40));
        Incident incident = restore(IncidentStatus.ASSIGNED, Priority.P2, null, null);
        LocalDateTime now = CREATED.plusSeconds(20);

        assertEquals(17, predictor.assess(incident, 0, now).score());
        assertEquals(new SlaRisk(RiskLevel.MEDIUM, 51,
                "The resolution deadline is due on 2026-11-19 at 10:00:40 and the incident is still unacknowledged."),
                custom.assess(incident, 0, now));
    }

    @Test
    void rejectsInvalidInputsAndUnclassifiedOpenIncidents() {
        Incident incident = restore(IncidentStatus.ASSIGNED, Priority.P4, null, null);
        Incident unclassified = restore(IncidentStatus.NEW, null, null, null);
        Incident earlierAcknowledgement = restore(IncidentStatus.IN_PROGRESS, Priority.P4, CREATED.minusNanos(1), null);
        Incident laterAcknowledgement = restore(IncidentStatus.IN_PROGRESS, Priority.P4, CREATED.plusNanos(1), null);

        assertThrows(IllegalArgumentException.class, () -> new HeuristicSlaRiskPredictor(null));
        assertThrows(IllegalArgumentException.class, () -> predictor.assess(null, 0, CREATED));
        assertThrows(IllegalArgumentException.class, () -> predictor.assess(incident, 0, null));
        assertThrows(IllegalArgumentException.class, () -> predictor.assess(incident, -1, CREATED));
        assertThrows(IllegalArgumentException.class, () -> predictor.assess(incident, 0, CREATED.minusNanos(1)));
        assertThrows(IllegalArgumentException.class, () -> predictor.assess(unclassified, 0, CREATED));
        assertThrows(IllegalArgumentException.class, () -> predictor.assess(earlierAcknowledgement, 0, CREATED));
        assertThrows(IllegalArgumentException.class, () -> predictor.assess(laterAcknowledgement, 0, CREATED));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void validatesTheResponseTargetOnlyWhileItIsOpen(long responseSeconds) {
        SlaRiskPredictor custom = predictorWithTargets(Duration.ofSeconds(responseSeconds), Duration.ofSeconds(40));
        Incident assigned = restore(IncidentStatus.ASSIGNED, Priority.P4, null, null);
        Incident acknowledged = restore(IncidentStatus.IN_PROGRESS, Priority.P4, CREATED, null);

        assertThrows(IllegalStateException.class, () -> custom.assess(assigned, 0, CREATED));
        assertEquals(0, custom.assess(acknowledged, 0, CREATED).score());
    }

    private static Incident restore(IncidentStatus status, Priority priority, LocalDateTime acknowledgedAt,
                                    LocalDateTime resolvedAt) {
        Technician technician = switch (status) {
            case NEW, CLASSIFIED -> null;
            default -> TECHNICIAN;
        };
        LocalDateTime closedAt = status == IncidentStatus.CLOSED ? resolvedAt.plusMinutes(5) : null;
        return Incident.restore("INC1", REPORTER, "Payment down", "Payments are failing", SERVICE,
                Impact.HIGH, Urgency.HIGH, 50, CREATED, status,
                IncidentCategory.APPLICATION, priority, technician, acknowledgedAt, resolvedAt, closedAt,
                resolvedAt == null ? null : "Restarted the service", closedAt == null ? null : "Confirmed by user",
                List.of(), null);
    }

    private static SlaRiskPredictor predictorWithTargets(Duration response, Duration resolution) {
        return new HeuristicSlaRiskPredictor(new FixedDeadlineCalculator(response, resolution));
    }

    private record FixedDeadlineCalculator(Duration response, Duration resolution) implements SlaCalculator {
        @Override
        public LocalDateTime responseDeadline(LocalDateTime createdAt, Priority priority) {
            return createdAt.plus(response);
        }

        @Override
        public LocalDateTime resolutionDeadline(LocalDateTime createdAt, Priority priority) {
            return createdAt.plus(resolution);
        }

        @Override
        public SlaStatus evaluate(LocalDateTime createdAt, Priority priority, LocalDateTime acknowledgedAt,
                                  LocalDateTime resolvedAt, LocalDateTime now) {
            throw new AssertionError("The predictor should use individual deadlines");
        }
    }
}
