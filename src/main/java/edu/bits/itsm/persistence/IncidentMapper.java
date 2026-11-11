package edu.bits.itsm.persistence;

import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Feedback;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.InvestigationNote;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.User;

import java.util.ArrayList;
import java.util.List;

// Translates between the domain Incident and the database row (IncidentEntity).
public class IncidentMapper {

    // The real technician objects. When loading, we pick from this list so the
    // technician is the same object the assignment strategy already knows about.
    private final List<Technician> technicians;

    public IncidentMapper(List<Technician> technicians) {
        this.technicians = technicians;
    }

    // ---------- Incident -> database row ----------

    public IncidentEntity toEntity(Incident incident) {
        IncidentEntity entity = new IncidentEntity();
        entity.id = incident.getId();

        Employee reporter = incident.getReporter();
        entity.reporterId = reporter.getId();
        entity.reporterName = reporter.getName();
        entity.reporterEmail = reporter.getEmail();
        entity.reporterDepartment = reporter.getDepartment();

        entity.title = incident.getTitle();
        entity.description = incident.getDescription();
        entity.serviceName = incident.getService().getName();
        entity.serviceCriticality = incident.getService().getCriticality();
        entity.impact = incident.getImpact();
        entity.urgency = incident.getUrgency();
        entity.affectedUsers = incident.getAffectedUsers();
        entity.createdAt = incident.getCreatedAt();
        entity.category = incident.getCategory();
        entity.priority = incident.getPriority();

        if (incident.getAssignedTechnician() != null) {
            entity.assignedTechnicianId = incident.getAssignedTechnician().getId();
        }

        entity.status = incident.getStatus();
        entity.acknowledgedAt = incident.getAcknowledgedAt();
        entity.resolvedAt = incident.getResolvedAt();
        entity.closedAt = incident.getClosedAt();
        entity.resolutionNote = incident.getResolutionNote();
        entity.closureNote = incident.getClosureNote();

        if (incident.getFeedback() != null) {
            entity.feedbackRating = incident.getFeedback().getRating();
            entity.feedbackComment = incident.getFeedback().getComment();
        }

        for (InvestigationNote note : incident.getInvestigationNotes()) {
            NoteData data = new NoteData();
            data.authorId = note.getAuthor().getId();
            data.authorName = note.getAuthor().getName();
            data.authorEmail = note.getAuthor().getEmail();
            data.authorRole = note.getAuthor().getRoleName();
            data.noteText = note.getText();
            data.createdAt = note.getCreatedAt();
            entity.notes.add(data);
        }

        return entity;
    }

    // ---------- database row -> Incident ----------

    public Incident toIncident(IncidentEntity entity) {
        Employee reporter = new Employee(entity.reporterId, entity.reporterName,
                entity.reporterEmail, entity.reporterDepartment);
        ItService service = new ItService(entity.serviceName, entity.serviceCriticality);

        Technician technician = null;
        if (entity.assignedTechnicianId != null) {
            technician = findTechnician(entity.assignedTechnicianId);
        }

        Feedback feedback = null;
        if (entity.feedbackRating != null) {
            feedback = new Feedback(entity.feedbackRating, entity.feedbackComment);
        }

        List<InvestigationNote> notes = new ArrayList<>();
        for (NoteData data : entity.notes) {
            User author = new SavedUser(data.authorId, data.authorName,
                    data.authorEmail, data.authorRole);
            notes.add(InvestigationNote.restore(author, data.noteText, data.createdAt));
        }

        return Incident.restore(entity.id, reporter, entity.title, entity.description,
                service, entity.impact, entity.urgency, entity.affectedUsers,
                entity.createdAt, entity.status, entity.category, entity.priority,
                technician, entity.acknowledgedAt, entity.resolvedAt, entity.closedAt,
                entity.resolutionNote, entity.closureNote, notes, feedback);
    }

    private Technician findTechnician(String id) {
        for (Technician t : technicians) {
            if (t.getId().equals(id)) {
                return t;
            }
        }
        throw new IllegalStateException("Technician " + id + " is no longer in the system");
    }

    // A note's author, rebuilt from the saved name, email and role.
    // We only need it to show who wrote the note.
    private static class SavedUser extends User {

        private final String role;

        SavedUser(String id, String name, String email, String role) {
            super(id, name, email);
            this.role = role;
        }

        @Override
        public String getRoleName() {
            return role;
        }
    }
}