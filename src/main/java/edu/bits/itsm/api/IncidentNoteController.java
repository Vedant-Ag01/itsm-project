package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.AddNoteRequest;
import edu.bits.itsm.api.dto.FeedbackRequest;
import edu.bits.itsm.api.dto.InfoMessageRequest;
import edu.bits.itsm.api.dto.IncidentResponse;
import edu.bits.itsm.api.dto.NoteResponse;
import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.ForbiddenException;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.auth.UserAccount;
import edu.bits.itsm.domain.Feedback;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.InvestigationNote;
import edu.bits.itsm.domain.Technician;
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
@RequestMapping("/api/incidents/{id}")
public class IncidentNoteController {

    private final IncidentService incidentService;

    public IncidentNoteController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/notes")
    public List<NoteResponse> notes(@PathVariable String id) {
        Incident incident = incidentService.getIncident(id);
        IncidentAccess.requireCanSee(CurrentUser.get(), incident);
        return notesOf(incident);
    }

    @PostMapping("/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public List<NoteResponse> addNote(@PathVariable String id,
                                      @RequestBody AddNoteRequest request) {
        UserAccount user = CurrentUser.get();
        IncidentAccess.requireCanSee(user, incidentService.getIncident(id));

        incidentService.addNote(id, new NoteAuthor(user), request.text());
        return notesOf(incidentService.getIncident(id));
    }

    @PostMapping("/feedback")
    @AllowedRoles({Role.EMPLOYEE, Role.ADMIN})
    public IncidentResponse giveFeedback(@PathVariable String id,
                                         @RequestBody FeedbackRequest request) {
        UserAccount user = CurrentUser.get();
        Incident incident = incidentService.getIncident(id);
        if (user.role != Role.ADMIN && !incident.getReporter().getId().equals(user.id)) {
            throw new ForbiddenException("Only the person who reported this incident can give feedback");
        }

        incidentService.giveFeedback(id, new Feedback(request.rating(), request.comment()));
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    // The assigned technician (or an admin) asks the reporter for more information
    @PostMapping("/request-info")
    @AllowedRoles({Role.TECHNICIAN, Role.ADMIN})
    public IncidentResponse requestInfo(@PathVariable String id,
                                        @RequestBody InfoMessageRequest request) {
        UserAccount user = CurrentUser.get();
        Incident incident = incidentService.getIncident(id);
        if (user.role != Role.ADMIN) {
            Technician assigned = incident.getAssignedTechnician();
            if (assigned == null || !assigned.getId().equals(user.id)) {
                throw new ForbiddenException("This incident is not assigned to you");
            }
        }
        incidentService.requestInformation(id, new NoteAuthor(user), request.message());
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    // The reporter (or an admin) answers the question
    @PostMapping("/provide-info")
    @AllowedRoles({Role.EMPLOYEE, Role.ADMIN})
    public IncidentResponse provideInfo(@PathVariable String id,
                                        @RequestBody InfoMessageRequest request) {
        UserAccount user = CurrentUser.get();
        Incident incident = incidentService.getIncident(id);
        if (user.role != Role.ADMIN && !incident.getReporter().getId().equals(user.id)) {
            throw new ForbiddenException("Only the person who reported this incident can answer");
        }
        incidentService.provideInformation(id, new NoteAuthor(user), request.message());
        return IncidentResponse.from(incidentService.getIncident(id));
    }

    private List<NoteResponse> notesOf(Incident incident) {
        List<NoteResponse> responses = new ArrayList<>();
        for (InvestigationNote note : incident.getInvestigationNotes()) {
            responses.add(NoteResponse.from(note));
        }
        return responses;
    }
}