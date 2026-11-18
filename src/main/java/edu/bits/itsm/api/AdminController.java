package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.AssignRequest;
import edu.bits.itsm.api.dto.AvailabilityRequest;
import edu.bits.itsm.api.dto.IncidentResponse;
import edu.bits.itsm.api.dto.TechnicianResponse;
import edu.bits.itsm.api.dto.UserResponse;
import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.auth.UserAccount;
import edu.bits.itsm.auth.UserAccountTable;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.TechnicianUnavailableException;
import edu.bits.itsm.service.IncidentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

// Admin-only tools: people, technician availability and manual assignment
@RestController
@RequestMapping("/api")
public class AdminController {

    private final IncidentService incidentService;
    private final List<Technician> technicians;
    private final UserAccountTable users;

    public AdminController(IncidentService incidentService,
                           List<Technician> technicians,
                           UserAccountTable users) {
        this.incidentService = incidentService;
        this.technicians = technicians;
        this.users = users;
    }

    @GetMapping("/admin/users")
    @AllowedRoles({Role.ADMIN})
    public List<UserResponse> listUsers() {
        List<UserResponse> responses = new ArrayList<>();
        for (UserAccount account : users.findAll()) {
            responses.add(UserResponse.from(account));
        }
        return responses;
    }

    @GetMapping("/admin/technicians")
    @AllowedRoles({Role.ADMIN})
    public List<TechnicianResponse> listTechnicians() {
        List<TechnicianResponse> responses = new ArrayList<>();
        for (Technician technician : technicians) {
            responses.add(TechnicianResponse.from(technician, activeIncidentsOf(technician)));
        }
        return responses;
    }

    @PutMapping("/admin/technicians/{id}/availability")
    @AllowedRoles({Role.ADMIN})
    public TechnicianResponse setAvailability(@PathVariable String id,
                                              @RequestBody AvailabilityRequest request) {
        Technician technician = findTechnician(id);
        technician.setAvailable(request.available());
        return TechnicianResponse.from(technician, activeIncidentsOf(technician));
    }

    @PostMapping("/incidents/{id}/assign")
    @AllowedRoles({Role.ADMIN})
    public IncidentResponse assign(@PathVariable String id,
                                   @RequestBody AssignRequest request) {
        UserAccount admin = CurrentUser.get();
        Technician technician = findTechnician(request.technicianId());
        if (!technician.isAvailable()) {
            throw new TechnicianUnavailableException(id);
        }
        incidentService.assignTo(id, technician, admin.name);
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    private Technician findTechnician(String id) {
        for (Technician technician : technicians) {
            if (technician.getId().equals(id)) {
                return technician;
            }
        }
        throw new IllegalArgumentException("Unknown technician '" + id + "'");
    }

    // Active means the technician is still working on it
    private int activeIncidentsOf(Technician technician) {
        int count = 0;
        for (Incident incident : incidentService.getAllIncidents()) {
            Technician assigned = incident.getAssignedTechnician();
            if (assigned == null || !assigned.getId().equals(technician.getId())) {
                continue;
            }
            IncidentStatus status = incident.getStatus();
            if (status == IncidentStatus.ASSIGNED
                    || status == IncidentStatus.ACKNOWLEDGED
                    || status == IncidentStatus.IN_PROGRESS) {
                count++;
            }
        }
        return count;
    }
}