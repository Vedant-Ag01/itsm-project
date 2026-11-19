package edu.bits.itsm.api;

import edu.bits.itsm.audit.AuditRecord;
import edu.bits.itsm.audit.AuditRepository;
import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.service.IncidentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuditController {

    private final AuditRepository auditRepository;
    private final IncidentService incidentService;

    public AuditController(AuditRepository auditRepository, IncidentService incidentService) {
        this.auditRepository = auditRepository;
        this.incidentService = incidentService;
    }

    // History of one incident, for anyone allowed to see that incident
    @GetMapping("/api/incidents/{id}/audit")
    public List<AuditRecord> forIncident(@PathVariable String id) {
        Incident incident = incidentService.getIncident(id);
        IncidentAccess.requireCanSee(CurrentUser.get(), incident);
        return auditRepository.findByIncident(id);
    }

    // Everything that happened, for the admin screen
    @GetMapping("/api/admin/audit")
    @AllowedRoles({Role.ADMIN})
    public List<AuditRecord> everything() {
        return auditRepository.findAll();
    }
}