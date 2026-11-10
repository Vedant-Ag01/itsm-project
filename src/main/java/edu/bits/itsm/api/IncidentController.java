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
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.duplicate.DuplicateMatch;
import edu.bits.itsm.service.CreationResult;
import edu.bits.itsm.service.IncidentFilter;
import edu.bits.itsm.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    // Spring sees this constructor and hands in the IncidentService bean from AppConfig
    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    // ---------- creating and reading ----------

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

    // All filters are optional. Example: /api/incidents?status=RESOLVED&q=vpn
    @GetMapping
    public List<IncidentResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String technicianId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        UserAccount user = CurrentUser.get();
        IncidentFilter filter = new IncidentFilter(
                parseEnum(IncidentStatus.class, status, "status"),
                parseEnum(Priority.class, priority, "priority"),
                parseEnum(IncidentCategory.class, category, "category"),
                technicianId,
                q,
                parseDate(from, "from"),
                parseDate(to, "to"));

        List<Incident> found = new ArrayList<>();
        for (Incident incident : incidentService.getAllIncidents()) {
            if (canSee(user, incident) && filter.matches(incident)) {
                found.add(incident);
            }
        }

        // newest first
        found.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        List<IncidentResponse> responses = new ArrayList<>();
        for (Incident incident : found) {
            responses.add(IncidentResponse.from(incident));
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

    // ---------- turning the text in the URL into real values ----------

    // "resolved" or "RESOLVED" both work. An empty value means "no filter".
    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown " + fieldName + " '" + value
                    + "'. Allowed: " + Arrays.toString(type.getEnumConstants()));
        }
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("'" + fieldName
                    + "' must be a date like 2026-11-10");
        }
    }
}