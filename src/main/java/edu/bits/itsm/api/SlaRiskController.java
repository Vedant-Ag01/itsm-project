package edu.bits.itsm.api;

import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.service.IncidentService;
import edu.bits.itsm.sla.RiskLevel;
import edu.bits.itsm.sla.SlaRisk;
import edu.bits.itsm.sla.SlaRiskPredictor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// "Which incidents are likely to miss their SLA?" (before they actually do)
@RestController
public class SlaRiskController {

    public record RiskResponse(String incidentId, String title, String priority, String status,
                               String technician, String level, int score, String reason) { }

    private final IncidentService incidentService;
    private final SlaRiskPredictor predictor;

    public SlaRiskController(IncidentService incidentService, SlaRiskPredictor predictor) {
        this.incidentService = incidentService;
        this.predictor = predictor;
    }

    @GetMapping("/api/incidents/{id}/sla-risk")
    public RiskResponse one(@PathVariable String id) {
        Incident incident = incidentService.getIncident(id);
        IncidentAccess.requireCanSee(CurrentUser.get(), incident);
        return toResponse(incident, assess(incident));
    }

    // Open incidents, riskiest first. Optional ?minLevel=MEDIUM or HIGH to hide the harmless ones.
    @GetMapping("/api/admin/sla-risks")
    @AllowedRoles({Role.ADMIN})
    public List<RiskResponse> atRisk(@RequestParam(required = false) String minLevel) {
        RiskLevel minimum = RiskLevel.LOW;
        if (minLevel != null && !minLevel.isBlank()) {
            try {
                minimum = RiskLevel.valueOf(minLevel.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown minLevel '" + minLevel
                        + "'. Allowed: LOW, MEDIUM, HIGH");
            }
        }

        List<RiskResponse> result = new ArrayList<>();
        for (Incident incident : incidentService.getAllIncidents()) {
            IncidentStatus status = incident.getStatus();
            if (status == IncidentStatus.RESOLVED || status == IncidentStatus.VERIFIED
                    || status == IncidentStatus.CLOSED) {
                continue;
            }
            try {
                SlaRisk risk = assess(incident);
                if (risk.level().compareTo(minimum) >= 0) {
                    result.add(toResponse(incident, risk));
                }
            } catch (RuntimeException e) {
                // an incident that cannot be judged (for example no priority yet) is skipped
            }
        }
        result.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return result;
    }

    private SlaRisk assess(Incident incident) {
        Technician assigned = incident.getAssignedTechnician();
        int load = assigned == null ? 0 : incidentService.activeLoadOf(assigned);
        return predictor.assess(incident, load, LocalDateTime.now());
    }

    private RiskResponse toResponse(Incident incident, SlaRisk risk) {
        String technician = incident.getAssignedTechnician() == null
                ? null : incident.getAssignedTechnician().getName();
        String priority = incident.getPriority() == null ? null : incident.getPriority().name();
        return new RiskResponse(incident.getId(), incident.getTitle(), priority,
                incident.getStatus().name(), technician, risk.level().name(),
                risk.score(), risk.reason());
    }
}
