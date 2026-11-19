package edu.bits.itsm.api;

import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.correlation.CorrelationGroup;
import edu.bits.itsm.correlation.IncidentCorrelator;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.service.IncidentService;
import edu.bits.itsm.suggestion.ResolutionSuggester;
import edu.bits.itsm.suggestion.ResolutionSuggestion;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

// "Is something bigger going on?" (correlation) and "Has this been fixed before?" (suggestions)
@RestController
public class IntelligenceController {

    public record GroupIncident(String id, String title, String status, String priority, LocalDateTime createdAt) { }

    public record GroupResponse(String serviceName, String category, LocalDateTime windowStart,
                                LocalDateTime windowEnd, int count, String summary,
                                List<GroupIncident> incidents) { }

    private final IncidentService incidentService;
    private final IncidentCorrelator correlator;
    private final ResolutionSuggester suggester;

    public IntelligenceController(IncidentService incidentService, IncidentCorrelator correlator,
                                  ResolutionSuggester suggester) {
        this.incidentService = incidentService;
        this.correlator = correlator;
        this.suggester = suggester;
    }

    // Possible outages: several open incidents on the same service close together in time
    @GetMapping("/api/admin/correlations")
    @AllowedRoles({Role.ADMIN})
    public List<GroupResponse> correlations() {
        List<CorrelationGroup> groups = correlator.findGroups(incidentService.getAllIncidents(), LocalDateTime.now());
        return groups.stream().map(IntelligenceController::toResponse).toList();
    }

    // Past resolved incidents that look like this one, with the note that fixed them
    @GetMapping("/api/incidents/{id}/resolution-suggestions")
    @AllowedRoles({Role.TECHNICIAN, Role.ADMIN})
    public List<ResolutionSuggestion> suggestions(@PathVariable String id,
                                                  @RequestParam(defaultValue = "3") int limit) {
        Incident incident = incidentService.getIncident(id);
        IncidentAccess.requireCanSee(CurrentUser.get(), incident);
        int capped = Math.max(1, Math.min(limit, 10));
        return suggester.suggest(incident, incidentService.getAllIncidents(), capped);
    }

    private static GroupResponse toResponse(CorrelationGroup g) {
        List<GroupIncident> items = g.incidents().stream()
                .map(i -> new GroupIncident(i.getId(), i.getTitle(), i.getStatus().name(),
                        i.getPriority() == null ? null : i.getPriority().name(), i.getCreatedAt()))
                .toList();
        return new GroupResponse(g.serviceName(), g.category() == null ? null : g.category().name(),
                g.windowStart(), g.windowEnd(), g.size(), g.summary(), items);
    }
}
