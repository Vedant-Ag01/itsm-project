package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.ActionRequest;
import edu.bits.itsm.api.dto.NoteActionRequest;
import edu.bits.itsm.api.dto.CreateIncidentRequest;
import edu.bits.itsm.api.dto.CreateIncidentResponse;
import edu.bits.itsm.api.dto.IncidentResponse;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.ItService;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateIncidentResponse create(@RequestBody CreateIncidentRequest request) {
        // Temporary reporter until login exists (V8)
        Employee reporter = new Employee("TEMP", request.reporterName(),
                "temp@bits.edu", "Unknown");
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
        List<IncidentResponse> responses = new ArrayList<>();
        for (Incident incident : incidentService.getAllIncidents()) {
            responses.add(IncidentResponse.from(incident));
        }
        return responses;
    }

    @GetMapping("/{id}")
    public IncidentResponse getOne(@PathVariable String id) {
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/auto-assign")
    public IncidentResponse autoAssign(@PathVariable String id,
                                       @RequestBody(required = false) ActionRequest request) {
        incidentService.autoAssign(id, actorOf(request));
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/acknowledge")
    public IncidentResponse acknowledge(@PathVariable String id,
                                        @RequestBody(required = false) ActionRequest request) {
        incidentService.acknowledge(id, actorOf(request));
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/start-work")
    public IncidentResponse startWork(@PathVariable String id,
                                      @RequestBody(required = false) ActionRequest request) {
        incidentService.startWork(id, actorOf(request));
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/resolve")
    public IncidentResponse resolve(@PathVariable String id,
                                    @RequestBody NoteActionRequest request) {
        incidentService.resolve(id, request.actor(), request.note());
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/verify")
    public IncidentResponse verify(@PathVariable String id,
                                   @RequestBody(required = false) ActionRequest request) {
        incidentService.verify(id, actorOf(request));
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    @PostMapping("/{id}/close")
    public IncidentResponse close(@PathVariable String id,
                                  @RequestBody NoteActionRequest request) {
        incidentService.close(id, request.actor(), request.note());
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    // If nobody says who is acting, call it "Unknown"
    private String actorOf(ActionRequest request) {
        if (request == null || request.actor() == null) {
            return "Unknown";
        }
        return request.actor();
    }
}