package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.AssignRequest;
import edu.bits.itsm.api.dto.AvailabilityRequest;
import edu.bits.itsm.api.dto.IncidentResponse;
import edu.bits.itsm.api.dto.TechnicianResponse;
import edu.bits.itsm.api.dto.UserResponse;
import edu.bits.itsm.assignment.AssignmentStrategy;
import edu.bits.itsm.assignment.LoadBasedStrategy;
import edu.bits.itsm.assignment.SkillBasedStrategy;
import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.auth.UserAccount;
import edu.bits.itsm.auth.UserAccountTable;
import edu.bits.itsm.auth.UserManagementService;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.TechnicianUnavailableException;
import edu.bits.itsm.service.IncidentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Admin-only tools: people, technician availability, manual assignment, assignment rule
@RestController
@RequestMapping("/api")
public class AdminController {

    public record StrategyRequest(String name) { }

    public record CreateTechnicianRequest(String name, String email, String department,
                                          String password, List<String> skills) { }

    private final IncidentService incidentService;
    private final List<Technician> technicians;
    private final UserAccountTable users;
    private final UserManagementService userService;

    // the assignment rules an admin can choose from
    private final Map<String, AssignmentStrategy> strategies = new LinkedHashMap<>();

    public AdminController(IncidentService incidentService,
                           List<Technician> technicians,
                           UserAccountTable users,
                           UserManagementService userService) {
        this.incidentService = incidentService;
        this.technicians = technicians;
        this.users = users;
        this.userService = userService;

        AssignmentStrategy skillBased = new SkillBasedStrategy();
        AssignmentStrategy loadBased = new LoadBasedStrategy();
        strategies.put(skillBased.getName(), skillBased);
        strategies.put(loadBased.getName(), loadBased);
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

    // Makes a technician login and adds the technician to the list used for assignment
    @PostMapping("/admin/technicians")
    @AllowedRoles({Role.ADMIN})
    @ResponseStatus(HttpStatus.CREATED)
    public TechnicianResponse createTechnician(@RequestBody CreateTechnicianRequest request) {
        // check the skill names first, so a typo does not leave a half-made account behind
        List<IncidentCategory> categories = new ArrayList<>();
        if (request.skills() != null) {
            for (String skill : request.skills()) {
                try {
                    categories.add(IncidentCategory.valueOf(skill));
                } catch (IllegalArgumentException | NullPointerException e) {
                    throw new IllegalArgumentException("Unknown skill '" + skill + "'");
                }
            }
        }

        UserAccount account = userService.createTechnician(request.name(), request.email(),
                request.department(), request.password(), request.skills());

        Technician technician = new Technician(account.id, account.name, account.email);
        for (IncidentCategory category : categories) {
            technician.addSkill(category);
        }
        technicians.add(technician);
        return TechnicianResponse.from(technician, 0);
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

    // ---------- which rule auto-assign uses ----------

    @GetMapping("/admin/assignment-strategy")
    @AllowedRoles({Role.ADMIN})
    public Map<String, Object> currentStrategy() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("current", incidentService.getAssignmentStrategyName());
        result.put("available", new ArrayList<>(strategies.keySet()));
        return result;
    }

    @PutMapping("/admin/assignment-strategy")
    @AllowedRoles({Role.ADMIN})
    public Map<String, Object> changeStrategy(@RequestBody StrategyRequest request) {
        AssignmentStrategy chosen = request.name() == null ? null : strategies.get(request.name());
        if (chosen == null) {
            throw new IllegalArgumentException("Unknown strategy '" + request.name()
                    + "'. Allowed: " + strategies.keySet());
        }
        incidentService.changeAssignmentStrategy(chosen);
        return currentStrategy();
    }

    // ---------- helpers ----------

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