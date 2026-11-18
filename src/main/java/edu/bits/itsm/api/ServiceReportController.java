package edu.bits.itsm.api;

import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.service.IncidentService;
import edu.bits.itsm.service.ServiceAnalysisReport;
import edu.bits.itsm.sla.SlaCalculator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class ServiceReportController {

    private final IncidentService incidentService;
    private final SlaCalculator slaCalculator;

    public ServiceReportController(IncidentService incidentService, SlaCalculator slaCalculator) {
        this.incidentService = incidentService;
        this.slaCalculator = slaCalculator;
    }

    // Which services cause the most incidents, SLA breaches and P1s
    @GetMapping("/api/admin/reports/services")
    @AllowedRoles({Role.ADMIN})
    public List<ServiceAnalysisReport.Entry> serviceAnalysis() {
        return ServiceAnalysisReport.build(incidentService.getAllIncidents(), this::slaStatusOf);
    }

    private String slaStatusOf(Incident incident) {
        try {
            return slaCalculator.evaluate(incident.getCreatedAt(), incident.getPriority(),
                    incident.getAcknowledgedAt(), incident.getResolvedAt(),
                    LocalDateTime.now()).name();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
