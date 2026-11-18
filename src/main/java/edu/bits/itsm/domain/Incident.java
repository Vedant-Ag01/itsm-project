package edu.bits.itsm.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Incident {

    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;
    public static final int MAX_AFFECTED_USERS = 100000;

    // ---------- set once, never changes ----------
    // (title, description, impact, urgency and affectedUsers can be corrected
    //  with update() until the incident is resolved)
    private final String id;
    private final Employee reporter;
    private String title;
    private String description;
    private final ItService service;
    private Impact impact;
    private Urgency urgency;
    private int affectedUsers;
    private final LocalDateTime createdAt;

    // ---------- filled in as the incident progresses ----------
    private IncidentCategory category;
    private Priority priority;
    private Technician assignedTechnician;

    // The current state object. Status is read from it, and it changes only
    // through the action methods below.
    private IncidentState state = new NewState();

    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private String resolutionNote;
    private String closureNote;
    private final List<InvestigationNote> investigationNotes = new ArrayList<>();
    private Feedback feedback;

    public Incident(String id, Employee reporter, String title, String description,
                    ItService service, Impact impact, Urgency urgency, int affectedUsers) {
        this(id, reporter, title, description, service, impact, urgency,
                affectedUsers, LocalDateTime.now());
    }

    // Used by the public constructor and by restore()
    private Incident(String id, Employee reporter, String title, String description,
                     ItService service, Impact impact, Urgency urgency, int affectedUsers,
                     LocalDateTime createdAt) {
        if (id == null || id.isBlank()) {
            throw new InvalidIncidentException("Incident id cannot be empty");
        }
        if (reporter == null) {
            throw new InvalidIncidentException("Incident must have a reporter");
        }
        if (title == null || title.isBlank()) {
            throw new InvalidIncidentException("Incident title cannot be empty");
        }
        if (description == null || description.isBlank()) {
            throw new InvalidIncidentException("Incident description cannot be empty");
        }
        if (service == null) {
            throw new InvalidIncidentException("Incident must name the affected service");
        }
        if (impact == null || urgency == null) {
            throw new InvalidIncidentException("Incident must have impact and urgency");
        }
        if (affectedUsers < 1) {
            throw new InvalidIncidentException("At least one user must be affected");
        }
        this.id = id;
        this.reporter = reporter;
        this.title = title;
        this.description = description;
        this.service = service;
        this.impact = impact;
        this.urgency = urgency;
        this.affectedUsers = affectedUsers;
        this.createdAt = createdAt;
    }

    // ---------- rebuilding a saved incident (for the database layer only) ----------

    // Normal code must NOT use this. It sets saved facts directly and skips
    // the lifecycle rules, because the incident already went through them.
    public static Incident restore(String id, Employee reporter, String title,
                                   String description, ItService service, Impact impact,
                                   Urgency urgency, int affectedUsers, LocalDateTime createdAt,
                                   IncidentStatus status, IncidentCategory category,
                                   Priority priority, Technician assignedTechnician,
                                   LocalDateTime acknowledgedAt, LocalDateTime resolvedAt,
                                   LocalDateTime closedAt, String resolutionNote,
                                   String closureNote, List<InvestigationNote> notes,
                                   Feedback feedback) {
        if (createdAt == null || status == null) {
            throw new InvalidIncidentException("Saved incident is missing its date or status");
        }
        Incident incident = new Incident(id, reporter, title, description, service,
                impact, urgency, affectedUsers, createdAt);
        incident.state = stateFor(status);
        incident.category = category;
        incident.priority = priority;
        incident.assignedTechnician = assignedTechnician;
        incident.acknowledgedAt = acknowledgedAt;
        incident.resolvedAt = resolvedAt;
        incident.closedAt = closedAt;
        incident.resolutionNote = resolutionNote;
        incident.closureNote = closureNote;
        if (notes != null) {
            incident.investigationNotes.addAll(notes);
        }
        incident.feedback = feedback;
        return incident;
    }

    // Picks the state object that matches a saved status
    private static IncidentState stateFor(IncidentStatus status) {
        switch (status) {
            case CLASSIFIED:
                return new ClassifiedState();
            case ASSIGNED:
                return new AssignedState();
            case ACKNOWLEDGED:
                return new AcknowledgedState();
            case IN_PROGRESS:
                return new InProgressState();
            case RESOLVED:
                return new ResolvedState();
            case VERIFIED:
                return new VerifiedState();
            case CLOSED:
                return new ClosedState();
            default:
                return new NewState();
        }
    }

    // ---------- lifecycle actions (the only way status changes) ----------

    public void classify() {
        state = state.classify();
    }

    public void assign(Technician technician) {
        if (technician == null) {
            throw new IllegalArgumentException("Cannot assign to a missing technician");
        }
        state = state.assign();              // throws first if illegal
        this.assignedTechnician = technician;
    }

    public void acknowledge() {
        state = state.acknowledge();
        this.acknowledgedAt = LocalDateTime.now();
    }

    public void startWork() {
        state = state.startWork();
    }

    public void resolve(String resolutionNote) {
        if (resolutionNote == null || resolutionNote.isBlank()) {
            throw new InvalidIncidentException("Resolution details are required");
        }
        state = state.resolve();
        this.resolutionNote = resolutionNote;
        this.resolvedAt = LocalDateTime.now();
    }

    public void verify() {
        state = state.verify();
    }

    public void close(String closureNote) {
        if (closureNote == null || closureNote.isBlank()) {
            throw new InvalidIncidentException("Closure information is required");
        }
        state = state.close();
        this.closureNote = closureNote;
        this.closedAt = LocalDateTime.now();
    }

    // The reporter (or an admin) corrects the details. Anything passed as null stays as it is.
    // Allowed only until the incident is resolved: after that the facts are history.
    public void update(String title, String description, Impact impact,
                       Urgency urgency, Integer affectedUsers) {
        IncidentStatus status = getStatus();
        if (status == IncidentStatus.RESOLVED
                || status == IncidentStatus.VERIFIED
                || status == IncidentStatus.CLOSED) {
            throw new InvalidStateTransitionException(status, "edit");
        }
        if (title != null && title.isBlank()) {
            throw new InvalidIncidentException("Incident title cannot be empty");
        }
        if (description != null && description.isBlank()) {
            throw new InvalidIncidentException("Incident description cannot be empty");
        }
        if (affectedUsers != null && affectedUsers < 1) {
            throw new InvalidIncidentException("At least one user must be affected");
        }
        checkFieldSizes(title, description, affectedUsers);
        // all checks passed, now change things
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (impact != null) {
            this.impact = impact;
        }
        if (urgency != null) {
            this.urgency = urgency;
        }
        if (affectedUsers != null) {
            this.affectedUsers = affectedUsers;
        }
    }

    // Size limits, so the database never receives something too long or absurd.
    // Called when an incident is reported and when it is edited.
    public static void checkFieldSizes(String title, String description, Integer affectedUsers) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new InvalidIncidentException(
                    "Title must be at most " + MAX_TITLE_LENGTH + " characters");
        }
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidIncidentException(
                    "Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        if (affectedUsers != null && affectedUsers > MAX_AFFECTED_USERS) {
            throw new InvalidIncidentException(
                    "Affected users cannot be more than " + MAX_AFFECTED_USERS);
        }
    }

    // ---------- other actions ----------

    public void addInvestigationNote(InvestigationNote note) {
        if (note == null) {
            throw new InvalidIncidentException("Note cannot be empty");
        }
        if (getStatus() == IncidentStatus.CLOSED) {
            throw new InvalidStateTransitionException(getStatus(), "add a note to");
        }
        investigationNotes.add(note);
    }

    public void giveFeedback(Feedback feedback) {
        if (feedback == null) {
            throw new InvalidIncidentException("Feedback cannot be empty");
        }
        IncidentStatus status = getStatus();
        if (status != IncidentStatus.RESOLVED
                && status != IncidentStatus.VERIFIED
                && status != IncidentStatus.CLOSED) {
            throw new InvalidStateTransitionException(status, "give feedback on");
        }
        this.feedback = feedback;
    }

    // ---------- getters and the two simple setters ----------

    public String getId() {
        return id;
    }

    public Employee getReporter() {
        return reporter;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ItService getService() {
        return service;
    }

    public Impact getImpact() {
        return impact;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public int getAffectedUsers() {
        return affectedUsers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public IncidentStatus getStatus() {
        return state.getStatus();
    }

    public Technician getAssignedTechnician() {
        return assignedTechnician;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public String getClosureNote() {
        return closureNote;
    }

    public List<InvestigationNote> getInvestigationNotes() {
        return Collections.unmodifiableList(investigationNotes);
    }

    // True when a technician asked for more information and the reporter has not answered yet
    public boolean isAwaitingInfo() {
        for (int i = investigationNotes.size() - 1; i >= 0; i--) {
            NoteType type = investigationNotes.get(i).getType();
            if (type == NoteType.INFO_REQUEST) {
                return true;
            }
            if (type == NoteType.INFO_RESPONSE) {
                return false;
            }
        }
        return false;
    }

    public Feedback getFeedback() {
        return feedback;
    }
}