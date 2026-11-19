package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.CreateIncidentRequest;
import edu.bits.itsm.api.dto.CreateIncidentResponse;
import edu.bits.itsm.api.dto.IncidentResponse;
import edu.bits.itsm.api.dto.NoteActionRequest;
import edu.bits.itsm.api.dto.UpdateIncidentRequest;
import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.classification.CategoryClassifier;
import edu.bits.itsm.classification.CategorySuggestion;
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
import edu.bits.itsm.domain.ServiceInfo;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.duplicate.DuplicateMatch;
import edu.bits.itsm.search.IncidentSearchCriteria;
import edu.bits.itsm.search.IncidentSearchService;
import edu.bits.itsm.service.CreationResult;
import edu.bits.itsm.service.IncidentService;
import edu.bits.itsm.service.ServiceCatalog;
import edu.bits.itsm.sla.SlaCalculator;
import edu.bits.itsm.sla.SlaStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentSearchService searchService;
    private final SlaCalculator slaCalculator;
    private final ServiceCatalog serviceCatalog;
    private final CategoryClassifier classifier;

    // Spring sees this constructor and hands in the beans from the config classes
    public IncidentController(IncidentService incidentService,
                              IncidentSearchService searchService,
                              SlaCalculator slaCalculator,
                              ServiceCatalog serviceCatalog,
                              CategoryClassifier classifier) {
        this.incidentService = incidentService;
        this.searchService = searchService;
        this.slaCalculator = slaCalculator;
        this.serviceCatalog = serviceCatalog;
        this.classifier = classifier;
    }

    // ---------- creating and reading ----------

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @AllowedRoles({Role.EMPLOYEE, Role.ADMIN})
    public CreateIncidentResponse create(@RequestBody CreateIncidentRequest request) {
        UserAccount user = CurrentUser.get();
        Employee reporter = new Employee(user.id, user.name, user.email, user.department);

        // The service must be in the catalogue, and its criticality comes from there,
        // not from the form, so the reporter cannot under-rate a critical service.
        ServiceInfo catalogued = serviceCatalog.requireActive(request.serviceName());
        ItService service = new ItService(catalogued.name(), catalogued.criticality());

        // If the form did not choose a category, work one out from the title and description
        IncidentCategory category = request.category();
        if (category == null) {
            category = classifier.suggest(request.title(), request.description())
                    .map(CategorySuggestion::category)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No category was given and none could be worked out from the text. "
                                    + "Please choose a category."));
        }

        CreationResult result = incidentService.createIncident(
                reporter,
                request.title(),
                request.description(),
                service,
                request.impact(),
                request.urgency(),
                request.affectedUsers(),
                category);

        List<String> duplicateIds = new ArrayList<>();
        for (DuplicateMatch match : result.possibleDuplicates()) {
            duplicateIds.add(match.other().getId());
        }

        return new CreateIncidentResponse(
                IncidentResponse.from(result.incident()), duplicateIds);
    }

    // All filters are optional. Example: /api/incidents?status=RESOLVED&q=vpn&slaStatus=BREACHED
    @GetMapping
    public List<IncidentResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String technicianId,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        UserAccount user = CurrentUser.get();

        IncidentSearchCriteria criteria = new IncidentSearchCriteria.Builder()
                .status(enumName(IncidentStatus.class, status, "status"))
                .priority(enumName(Priority.class, priority, "priority"))
                .category(enumName(IncidentCategory.class, category, "category"))
                .slaStatus(enumName(SlaStatus.class, slaStatus, "slaStatus"))
                .technicianId(technicianId)
                .service(service)
                .query(q)
                .fromDate(parseDate(from, "from"))
                .toDate(parseDate(to, "to"))
                .build();

        // first keep only what this user may see, then let the search service filter
        List<Incident> visible = new ArrayList<>();
        for (Incident incident : incidentService.getAllIncidents()) {
            if (IncidentAccess.canSee(user, incident)) {
                visible.add(incident);
            }
        }
        List<Incident> found = new ArrayList<>(
                searchService.search(visible, criteria, this::slaStatusOf));

        // newest first
        found.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        List<IncidentResponse> responses = new ArrayList<>();
        for (Incident incident : found) {
            responses.add(IncidentResponse.from(incident, slaStatusOf(incident)));
        }
        return responses;
    }

    @GetMapping("/{id}")
    public IncidentResponse getOne(@PathVariable String id) {
        Incident incident = incidentService.getIncident(id);
        IncidentAccess.requireCanSee(CurrentUser.get(), incident);
        return IncidentResponse.from(incident, slaStatusOf(incident));
    }

    // The reporter (or an admin) corrects the details. The priority is recalculated.
    @PutMapping("/{id}")
    @AllowedRoles({Role.EMPLOYEE, Role.ADMIN})
    public IncidentResponse update(@PathVariable String id,
                                   @RequestBody UpdateIncidentRequest request) {
        UserAccount user = CurrentUser.get();
        Incident incident = incidentService.getIncident(id);
        if (user.role != Role.ADMIN && !incident.getReporter().getId().equals(user.id)) {
            throw new ForbiddenException("Only the person who reported this incident can edit it");
        }
        Incident updated = incidentService.updateIncident(id, user.name, request.title(),
                request.description(), request.impact(), request.urgency(),
                request.affectedUsers());
        return IncidentResponse.from(updated);
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

    // ---------- helpers ----------

    private void requireAssignedTechnicianOrAdmin(UserAccount user, Incident incident) {
        if (user.role == Role.ADMIN) {
            return;
        }
        Technician assigned = incident.getAssignedTechnician();
        if (assigned == null || !assigned.getId().equals(user.id)) {
            throw new ForbiddenException("This incident is not assigned to you");
        }
    }

    // "ON_TRACK", "AT_RISK" or "BREACHED" right now (null if it can't be judged)
    private String slaStatusOf(Incident incident) {
        try {
            return slaCalculator.evaluate(incident.getCreatedAt(), incident.getPriority(),
                    incident.getAcknowledgedAt(), incident.getResolvedAt(),
                    LocalDateTime.now()).name();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // "resolved" or "RESOLVED" both work. An empty value means "no filter".
    // Returns the proper name, or throws a clear error for an unknown value.
    private <E extends Enum<E>> String enumName(Class<E> type, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase()).name();
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