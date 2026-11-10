package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.CreateIncidentRequest;
import edu.bits.itsm.api.dto.CreateIncidentResponse;
import edu.bits.itsm.api.dto.IncidentResponse;
import edu.bits.itsm.api.dto.NoteActionRequest;
import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.ForbiddenException;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.auth.UserAccount;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.duplicate.DuplicateMatch;
import edu.bits.itsm.service.CreationResult;
import edu.bits.itsm.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    // Spring sees this constructor and hands in the IncidentService bean from AppConfig
    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // ---------- reading ----------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AllowedRoles({Role.EMPLOYEE, Role.ADMIN})
    public CreateIncidentResponse create(@RequestBody CreateIncidentRequest request) {
        UserAccount user = CurrentUser.get();
        Employee reporter = new Employee(user.id, user.name, user.email, user.department);
        ItService service = new ItService(request.serviceName(), request.criticality());

        CreationResult result = incidentService.createIncident(
                reporter,
                request.title(),
                request.description(),
                service,
                request.impact(),
                request.urgency(),
                request.affectedUsers(),
                request.category());

        List<String> duplicateIds = new ArrayList<>();
        for (DuplicateMatch match : result.possibleDuplicates()) {
            duplicateIds.add(match.other().getId());
        }

        return new CreateIncidentResponse(
                IncidentResponse.from(result.incident()), duplicateIds);
    }

    @GetMapping
    public List<IncidentResponse> list() {
        UserAccount user = CurrentUser.get();
        List<IncidentResponse> responses = new ArrayList<>();
        for (Incident incident : incidentService.getAllIncidents()) {
            if (canSee(user, incident)) {
                responses.add(IncidentResponse.from(incident));
            }
        }
        return responses;
    }

    @GetMapping("/{id}")
    public IncidentResponse getOne(@PathVariable String id) {
        Incident incident = incidentService.getIncident(id);
        if (!canSee(CurrentUser.get(), incident)) {
            throw new ForbiddenException("You are not allowed to see this incident");
        }
        return IncidentResponse.from(incident);
    }

    // ---------- lifecycle actions ----------

    @PostMapping("/{id}/auto-assign")
    @AllowedRoles({Role.ADMIN})
    public IncidentResponse autoAssign(@PathVariable String id) {
        UserAccount user = CurrentUser.get();
        incidentService.autoAssign(id, user.name);
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/acknowledge")
    @AllowedRoles({Role.TECHNICIAN, Role.ADMIN})
    public IncidentResponse acknowledge(@PathVariable String id) {
        UserAccount user = CurrentUser.get();
        requireAssignedTechnicianOrAdmin(user, incidentService.getIncident(id));
        incidentService.acknowledge(id, user.name);
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/start-work")
    @AllowedRoles({Role.TECHNICIAN, Role.ADMIN})
    public IncidentResponse startWork(@PathVariable String id) {
        UserAccount user = CurrentUser.get();
        requireAssignedTechnicianOrAdmin(user, incidentService.getIncident(id));
        incidentService.startWork(id, user.name);
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/resolve")
    @AllowedRoles({Role.TECHNICIAN, Role.ADMIN})
    public IncidentResponse resolve(@PathVariable String id,
                                    @RequestBody NoteActionRequest request) {
        UserAccount user = CurrentUser.get();
        requireAssignedTechnicianOrAdmin(user, incidentService.getIncident(id));
        incidentService.resolve(id, user.name, request.note());
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/verify")
    @AllowedRoles({Role.EMPLOYEE, Role.ADMIN})
    public IncidentResponse verify(@PathVariable String id) {
        UserAccount user = CurrentUser.get();
        Incident incident = incidentService.getIncident(id);
        if (user.role != Role.ADMIN && !incident.getReporter().getId().equals(user.id)) {
            throw new ForbiddenException("Only the person who reported this incident can verify it");
        }
        incidentService.verify(id, user.name);
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/close")
    @AllowedRoles({Role.ADMIN})
    public IncidentResponse close(@PathVariable String id,
                                  @RequestBody NoteActionRequest request) {
        UserAccount user = CurrentUser.get();
        incidentService.close(id, user.name, request.note());
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    // ---------- who may see what ----------

    private boolean canSee(UserAccount user, Incident incident) {
        if (user.role == Role.ADMIN) {
            return true;
        }
        if (user.role == Role.EMPLOYEE) {
            return incident.getReporter().getId().equals(user.id);
        }
        Technician assigned = incident.getAssignedTechnician();
        return assigned != null && assigned.getId().equals(user.id);
    }

    private void requireAssignedTechnicianOrAdmin(UserAccount user, Incident incident) {
        if (user.role == Role.ADMIN) {
            return;
        }
        Technician assigned = incident.getAssignedTechnician();
        if (assigned == null || !assigned.getId().equals(user.id)) {
            throw new ForbiddenException("This incident is not assigned to you");
        }
    }
}