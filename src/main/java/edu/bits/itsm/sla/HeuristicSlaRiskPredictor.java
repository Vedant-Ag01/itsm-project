package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.Priority;

import java.time.Duration;
import java.time.LocalDateTime;

public class HeuristicSlaRiskPredictor implements SlaRiskPredictor {
    private final SlaCalculator calculator;

    public HeuristicSlaRiskPredictor(SlaCalculator calculator) {
        if (calculator == null) {
            throw new IllegalArgumentException("calculator must not be null");
        }
        this.calculator = calculator;
    }

    @Override
    public SlaRisk assess(Incident incident, int technicianActiveLoad, LocalDateTime now) {
        if (incident == null) {
            throw new IllegalArgumentException("incident must not be null");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        if (technicianActiveLoad < 0) {
            throw new IllegalArgumentException("technicianActiveLoad must not be negative");
        }

        LocalDateTime createdAt = incident.getCreatedAt();
        if (createdAt == null || now.isBefore(createdAt)) {
            throw new IllegalArgumentException("createdAt must be present and no later than now");
        }
        IncidentStatus status = incident.getStatus();
        if (status == IncidentStatus.RESOLVED || status == IncidentStatus.VERIFIED
                || status == IncidentStatus.CLOSED) {
            return new SlaRisk(RiskLevel.LOW, 0, "The incident is resolved and has no outstanding SLA deadline.");
        }

        Priority priority = incident.getPriority();
        if (priority == null) {
            throw new IllegalArgumentException("An open incident must have a priority before SLA assessment");
        }
        LocalDateTime acknowledgedAt = incident.getAcknowledgedAt();
        boolean unacknowledged = acknowledgedAt == null;
        if (!unacknowledged && (acknowledgedAt.isBefore(createdAt) || acknowledgedAt.isAfter(now))) {
            throw new IllegalArgumentException("acknowledgedAt must be between createdAt and now");
        }

        LocalDateTime deadline = calculator.resolutionDeadline(createdAt, priority);
        String deadlineKind = "resolution";
        validateDeadline(deadlineKind, deadline, createdAt);
        if (unacknowledged) {
            LocalDateTime responseDeadline = calculator.responseDeadline(createdAt, priority);
            validateDeadline("response", responseDeadline, createdAt);
            if (!responseDeadline.isAfter(deadline)) {
                deadline = responseDeadline;
                deadlineKind = "response";
            }
        }

        String reason = "The " + deadlineKind + " deadline is ";
        if (now.isAfter(deadline)) {
            return new SlaRisk(RiskLevel.HIGH, 100, reason + "already overdue.");
        }

        Duration elapsed = Duration.between(createdAt, now);
        Duration target = Duration.between(createdAt, deadline);
        int timePoints = (int) elapsed.multipliedBy(70).dividedBy(target);
        int priorityPoints = switch (priority) {
            case P1 -> 10;
            case P2 -> 6;
            case P3 -> 3;
            case P4 -> 0;
        };
        int acknowledgementPoints = unacknowledged ? 10 : 0;
        int loadPoints = 2 * Math.min(technicianActiveLoad, 5);
        int score = timePoints + priorityPoints + acknowledgementPoints + loadPoints;
        RiskLevel level = RiskLevel.LOW;
        if (score >= 70) {
            level = RiskLevel.HIGH;
        } else if (score >= 40) {
            level = RiskLevel.MEDIUM;
        }

        reason += now.equals(deadline)
                ? "due now"
                : "due on " + deadline.toLocalDate() + " at " + deadline.toLocalTime();
        if (unacknowledged) {
            reason += " and the incident is still unacknowledged";
        }
        return new SlaRisk(level, score, reason + ".");
    }

    private static void validateDeadline(String kind, LocalDateTime deadline, LocalDateTime createdAt) {
        if (deadline == null || !deadline.isAfter(createdAt)) {
            throw new IllegalStateException("The SLA " + kind + " deadline must be after incident creation");
        }
    }
}
