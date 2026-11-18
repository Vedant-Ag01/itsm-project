package edu.bits.itsm.service;

import edu.bits.itsm.assignment.AssignmentStrategy;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Feedback;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentNotFoundException;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.InvestigationNote;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.TechnicianUnavailableException;
import edu.bits.itsm.domain.Urgency;
import edu.bits.itsm.domain.User;
import edu.bits.itsm.duplicate.DuplicateDetector;
import edu.bits.itsm.duplicate.DuplicateMatch;
import edu.bits.itsm.event.IncidentEvent;
import edu.bits.itsm.event.IncidentEventType;
import edu.bits.itsm.event.IncidentObserver;
import edu.bits.itsm.priority.PriorityStrategy;
import edu.bits.itsm.repository.IncidentRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Runs the whole incident workflow. It does not hold any rules of its own:
// the lifecycle rules are in the Incident states, the priority and assignment
// rules are in the strategies, and everyone who wants to know what happened
// listens through the observers.
public class IncidentService {

    private final IncidentRepository repository;
    private final IncidentFactory factory;
    private final PriorityStrategy priorityStrategy;
    private AssignmentStrategy assignmentStrategy;   // not final: the admin can switch it
    private final DuplicateDetector duplicateDetector;
    private final List<IncidentObserver> observers;
    private final List<Technician> technicians;

    public IncidentService(IncidentRepository repository,
                           IncidentFactory factory,
                           PriorityStrategy priorityStrategy,
                           AssignmentStrategy assignmentStrategy,
                           DuplicateDetector duplicateDetector,
                           List<IncidentObserver> observers,
                           List<Technician> technicians) {
        this.repository = repository;
        this.factory = factory;
        this.priorityStrategy = priorityStrategy;
        this.assignmentStrategy = assignmentStrategy;
        this.duplicateDetector = duplicateDetector;
        this.observers = observers;
        this.technicians = technicians;
    }

    // ---------- creating an incident ----------

    public CreationResult createIncident(Employee reporter, String title, String description,
                                         ItService service, Impact impact, Urgency urgency,
                                         int affectedUsers, IncidentCategory category) {
        Incident incident = factory.create(reporter, title, description,
                service, impact, urgency, affectedUsers);
        publish(incident, IncidentEventType.CREATED, reporter.getName(), null,
                "Incident created by " + reporter.getName());

        // look for similar open incidents before saving this one
        List<DuplicateMatch> duplicates = duplicateDetector.findSimilar(incident, openIncidents());
        if (!duplicates.isEmpty()) {
            publish(incident, IncidentEventType.DUPLICATE_FLAGGED, "System", IncidentStatus.NEW,
                    "Looks similar to " + duplicates.size() + " open incident(s)");
        }

        incident.setCategory(category);
        incident.classify();
        publish(incident, IncidentEventType.CLASSIFIED, "System", IncidentStatus.NEW,
                "Incident classified as " + category);

        Priority priority = priorityStrategy.calculate(impact, urgency,
                service.getCriticality(), affectedUsers);
        incident.setPriority(priority);
        publish(incident, IncidentEventType.PRIORITY_CALCULATED, "System", IncidentStatus.CLASSIFIED,
                "Priority calculated as " + priority);

        repository.save(incident);
        return new CreationResult(incident, duplicates);
    }

    // ---------- assignment ----------

    // The system picks the technician using the current strategy.
    public Incident autoAssign(String incidentId, String actor) {
        Incident incident = findOrThrow(incidentId);
        List<Technician> candidates = availableTechnicians();
        Technician chosen = assignmentStrategy
                .selectTechnician(incident, candidates, currentLoad())
                .orElseThrow(() -> new TechnicianUnavailableException(incidentId));
        return assignTo(incidentId, chosen, actor);
    }

    // An admin picks the technician by hand.
    public Incident assignTo(String incidentId, Technician technician, String actor) {
        Incident incident = findOrThrow(incidentId);
        IncidentStatus before = incident.getStatus();
        incident.assign(technician);
        repository.save(incident);
        publish(incident, IncidentEventType.ASSIGNED, actor, before,
                "Assigned to " + technician.getName());
        return incident;
    }

    public void changeAssignmentStrategy(AssignmentStrategy newStrategy) {
        this.assignmentStrategy = newStrategy;
    }

    public String getAssignmentStrategyName() {
        return assignmentStrategy.getName();
    }

    // ---------- the rest of the lifecycle ----------

    public Incident acknowledge(String incidentId, String actor) {
        Incident incident = findOrThrow(incidentId);
        IncidentStatus before = incident.getStatus();
        incident.acknowledge();
        repository.save(incident);
        publish(incident, IncidentEventType.ACKNOWLEDGED, actor, before, "Incident acknowledged");
        return incident;
    }

    public Incident startWork(String incidentId, String actor) {
        Incident incident = findOrThrow(incidentId);
        IncidentStatus before = incident.getStatus();
        incident.startWork();
        repository.save(incident);
        publish(incident, IncidentEventType.WORK_STARTED, actor, before, "Investigation started");
        return incident;
    }

    public Incident resolve(String incidentId, String actor, String resolutionNote) {
        Incident incident = findOrThrow(incidentId);
        IncidentStatus before = incident.getStatus();
        incident.resolve(resolutionNote);
        repository.save(incident);
        publish(incident, IncidentEventType.RESOLVED, actor, before, "Resolved: " + resolutionNote);
        return incident;
    }

    public Incident verify(String incidentId, String actor) {
        Incident incident = findOrThrow(incidentId);
        IncidentStatus before = incident.getStatus();
        incident.verify();
        repository.save(incident);
        publish(incident, IncidentEventType.VERIFIED, actor, before, "Resolution verified");
        return incident;
    }

    public Incident close(String incidentId, String actor, String closureNote) {
        Incident incident = findOrThrow(incidentId);
        IncidentStatus before = incident.getStatus();
        incident.close(closureNote);
        repository.save(incident);
        publish(incident, IncidentEventType.CLOSED, actor, before, "Closed: " + closureNote);
        return incident;
    }

    // ---------- notes and feedback ----------

    public Incident addNote(String incidentId, User author, String text) {
        Incident incident = findOrThrow(incidentId);
        incident.addInvestigationNote(new InvestigationNote(author, text));
        repository.save(incident);
        return incident;
    }

    public Incident giveFeedback(String incidentId, Feedback feedback) {
        Incident incident = findOrThrow(incidentId);
        incident.giveFeedback(feedback);
        repository.save(incident);
        return incident;
    }

    // ---------- reading ----------

    public Incident getIncident(String incidentId) {
        return findOrThrow(incidentId);
    }

    public List<Incident> getAllIncidents() {
        return repository.findAll();
    }

    // ---------- private helpers ----------

    private Incident findOrThrow(String incidentId) {
        return repository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

    // tells every observer that something happened
    private void publish(Incident incident, IncidentEventType type, String actor,
                         IncidentStatus previousStatus, String description) {
        IncidentEvent event = new IncidentEvent(incident.getId(), type, actor,
                previousStatus, incident.getStatus(), description, LocalDateTime.now());
        for (IncidentObserver observer : observers) {
            observer.onEvent(event);
        }
    }

    private List<Incident> openIncidents() {
        List<Incident> open = new java.util.ArrayList<>();
        for (Incident incident : repository.findAll()) {
            if (incident.getStatus() != IncidentStatus.CLOSED) {
                open.add(incident);
            }
        }
        return open;
    }

    private List<Technician> availableTechnicians() {
        List<Technician> available = new java.util.ArrayList<>();
        for (Technician technician : technicians) {
            if (technician.isAvailable()) {
                available.add(technician);
            }
        }
        return available;
    }

    // how many active incidents each technician has right now
    private Map<Technician, Integer> currentLoad() {
        Map<Technician, Integer> load = new HashMap<>();
        for (Technician technician : technicians) {
            load.put(technician, 0);
        }
        for (Incident incident : repository.findAll()) {
            Technician technician = incident.getAssignedTechnician();
            if (technician != null && isActive(incident)) {
                load.put(technician, load.getOrDefault(technician, 0) + 1);
            }
        }
        return load;
    }

    private boolean isActive(Incident incident) {
        IncidentStatus status = incident.getStatus();
        return status == IncidentStatus.ASSIGNED
                || status == IncidentStatus.ACKNOWLEDGED
                || status == IncidentStatus.IN_PROGRESS;
    }
}