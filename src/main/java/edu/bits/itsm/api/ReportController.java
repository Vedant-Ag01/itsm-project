package edu.bits.itsm.api;

import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.reporting.CategoryBreakdown;
import edu.bits.itsm.reporting.IncidentOverviewReport;
import edu.bits.itsm.reporting.ReportService;
import edu.bits.itsm.reporting.WorkloadEntry;
import edu.bits.itsm.service.IncidentService;
import edu.bits.itsm.sla.SlaCalculator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ReportController {

    private final IncidentService incidentService;
    private final ReportService reportService;
    private final SlaCalculator slaCalculator;

    public ReportController(IncidentService incidentService,
                            ReportService reportService,
                            SlaCalculator slaCalculator) {
        this.incidentService = incidentService;
        this.reportService = reportService;
        this.slaCalculator = slaCalculator;
    }

    @GetMapping("/api/admin/reports/overview")
    @AllowedRoles({Role.ADMIN})
    public IncidentOverviewReport overview() {
        return reportService.overview(incidentService.getAllIncidents(), this::slaStatusOf);
    }

    @GetMapping("/api/admin/reports/workload")
    @AllowedRoles({Role.ADMIN})
    public List<WorkloadEntry> workload() {
        return reportService.technicianWorkload(incidentService.getAllIncidents());
    }

    @GetMapping("/api/admin/reports/categories")
    @AllowedRoles({Role.ADMIN})
    public List<CategoryBreakdown> categories() {
        return reportService.categoryBreakdown(incidentService.getAllIncidents());
    }

    @GetMapping("/api/admin/reports/resolution-time")
    @AllowedRoles({Role.ADMIN})
    public Map<String, Object> resolutionTime() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("averageResolutionMinutes",
                reportService.averageResolutionMinutes(incidentService.getAllIncidents()));
        return result;
    }

    // Deadlines and current SLA status of one incident
    @GetMapping("/api/incidents/{id}/sla")
    public Map<String, Object> sla(@PathVariable String id) {
        Incident incident = incidentService.getIncident(id);
        IncidentAccess.requireCanSee(CurrentUser.get(), incident);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("incidentId", incident.getId());
        result.put("priority", incident.getPriority());
        result.put("responseDeadline",
                slaCalculator.responseDeadline(incident.getCreatedAt(), incident.getPriority()));
        result.put("resolutionDeadline",
                slaCalculator.resolutionDeadline(incident.getCreatedAt(), incident.getPriority()));
        result.put("status", slaStatusOf(incident));
        return result;
    }

    // "ON_TRACK", "AT_RISK" or "BREACHED" for an incident right now (null if it can't be judged)
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