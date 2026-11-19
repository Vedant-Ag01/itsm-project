package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.IncidentResponse;
import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.service.IncidentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class WorkloadController {

    public record TechnicianWorkload(
            String technicianId,
            String name,
            boolean available,
            List<String> skills,
            int activeIncidents,
            int maxActiveIncidents,
            int remainingCapacity,
            Map<String, Integer> byStatus,
            List<IncidentResponse> incidents) { }

    public record WorkloadLimitRequest(int maxActiveIncidents) { }

    private final IncidentService incidentService;
    private final List<Technician> technicians;

    public WorkloadController(IncidentService incidentService, List<Technician> technicians) {
        this.incidentService = incidentService;
        this.technicians = technicians;
    }

    // A technician's own picture: how loaded they are and what is on their plate
    @GetMapping("/api/technician/workload")
    @AllowedRoles({Role.TECHNICIAN})
    public TechnicianWorkload myWorkload() {
        String userId = CurrentUser.get().id;
        Technician technician = null;
        for (Technician candidate : technicians) {
            if (candidate.getId().equals(userId)) {
                technician = candidate;
            }
        }
        if (technician == null) {
            throw new IllegalArgumentException("No technician profile exists for this account");
        }

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        byStatus.put(IncidentStatus.ASSIGNED.name(), 0);
        byStatus.put(IncidentStatus.ACKNOWLEDGED.name(), 0);
        byStatus.put(IncidentStatus.IN_PROGRESS.name(), 0);

        List<IncidentResponse> active = new ArrayList<>();
        for (Incident incident : incidentService.getAllIncidents()) {
            Technician assigned = incident.getAssignedTechnician();
            if (assigned == null || !assigned.getId().equals(technician.getId())) {
                continue;
            }
            String status = incident.getStatus().name();
            if (byStatus.containsKey(status)) {
                byStatus.put(status, byStatus.get(status) + 1);
                active.add(IncidentResponse.from(incident));
            }
        }

        List<String> skills = new ArrayList<>();
        for (IncidentCategory category : technician.getSkills()) {
            skills.add(category.name());
        }
        int limit = incidentService.getMaxActivePerTechnician();
        return new TechnicianWorkload(technician.getId(), technician.getName(),
                technician.isAvailable(), skills, active.size(), limit,
                Math.max(0, limit - active.size()), byStatus, active);
    }

    @GetMapping("/api/admin/workload-limit")
    @AllowedRoles({Role.ADMIN})
    public Map<String, Integer> getLimit() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("maxActiveIncidents", incidentService.getMaxActivePerTechnician());
        return result;
    }

    @PutMapping("/api/admin/workload-limit")
    @AllowedRoles({Role.ADMIN})
    public Map<String, Integer> setLimit(@RequestBody WorkloadLimitRequest request) {
        incidentService.setMaxActivePerTechnician(request.maxActiveIncidents());
        return getLimit();
    }
}
