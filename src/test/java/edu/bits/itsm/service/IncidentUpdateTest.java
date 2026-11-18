package edu.bits.itsm.service;

import edu.bits.itsm.assignment.AssignmentStrategy;
import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.InvalidIncidentException;
import edu.bits.itsm.domain.InvalidStateTransitionException;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.Urgency;
import edu.bits.itsm.duplicate.DuplicateDetector;
import edu.bits.itsm.duplicate.DuplicateMatch;
import edu.bits.itsm.event.IncidentEvent;
import edu.bits.itsm.event.IncidentEventType;
import edu.bits.itsm.event.IncidentObserver;
import edu.bits.itsm.priority.PriorityStrategy;
import edu.bits.itsm.repository.InMemoryIncidentRepository;
import edu.bits.itsm.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IncidentUpdateTest {

    // P1 when 10 or more users are affected, otherwise P3
    private static class UsersBasedPriority implements PriorityStrategy {
        public Priority calculate(Impact impact, Urgency urgency,
                                  BusinessCriticality criticality, int affectedUsers) {
            return affectedUsers >= 10 ? Priority.P1 : Priority.P3;
        }

        public String getName() {
            return "Users based";
        }
    }

    private static class FirstTechnician implements AssignmentStrategy {
        public Optional<Technician> selectTechnician(Incident incident,
                                                     List<Technician> candidates,
                                                     Map<Technician, Integer> activeLoad) {
            return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
        }

        public String getName() {
            return "First";
        }
    }

    private static class NoDuplicates implements DuplicateDetector {
        public List<DuplicateMatch> findSimilar(Incident incoming, List<Incident> existing) {
            return new ArrayList<>();
        }
    }

    private static class Recorder implements IncidentObserver {
        final List<IncidentEvent> events = new ArrayList<>();

        public void onEvent(IncidentEvent event) {
            events.add(event);
        }
    }

    private IncidentService service;
    private Recorder recorder;
    private Incident incident;

    @BeforeEach
    void setUp() {
        IncidentRepository repository = new InMemoryIncidentRepository();
        recorder = new Recorder();
        List<IncidentObserver> observers = new ArrayList<>();
        observers.add(recorder);
        List<Technician> technicians = new ArrayList<>();
        technicians.add(new Technician("T1", "Ravi", "ravi@bits.edu"));
        service = new IncidentService(repository, new IncidentFactory(repository),
                new UsersBasedPriority(), new FirstTechnician(), new NoDuplicates(),
                observers, technicians);

        Employee asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        ItService wifi = new ItService("Campus WiFi", BusinessCriticality.HIGH);
        incident = service.createIncident(asha, "WiFi slow", "WiFi is slow in the library",
                wifi, Impact.MEDIUM, Urgency.MEDIUM, 3, IncidentCategory.NETWORK).incident();
        recorder.events.clear();
    }

    @Test
    void changingAffectedUsersRecalculatesPriorityAndPublishesPriorityChanged() {
        assertEquals(Priority.P3, incident.getPriority());

        Incident updated = service.updateIncident(incident.getId(), "Asha",
                null, null, null, null, 40);

        assertEquals(40, updated.getAffectedUsers());
        assertEquals(Priority.P1, updated.getPriority());
        assertEquals(List.of(IncidentEventType.INCIDENT_UPDATED, IncidentEventType.PRIORITY_CHANGED),
                recorder.events.stream().map(IncidentEvent::type).toList());
    }

    @Test
    void anUpdateThatKeepsThePriorityDoesNotPublishPriorityChanged() {
        service.updateIncident(incident.getId(), "Asha", "WiFi very slow", null, null, null, null);

        assertEquals("WiFi very slow", service.getIncident(incident.getId()).getTitle());
        assertEquals(List.of(IncidentEventType.INCIDENT_UPDATED),
                recorder.events.stream().map(IncidentEvent::type).toList());
    }

    @Test
    void nullFieldsKeepTheirCurrentValues() {
        service.updateIncident(incident.getId(), "Asha", null, "Now also in the hostel",
                null, null, null);

        Incident saved = service.getIncident(incident.getId());
        assertEquals("WiFi slow", saved.getTitle());
        assertEquals("Now also in the hostel", saved.getDescription());
        assertEquals(Impact.MEDIUM, saved.getImpact());
        assertEquals(3, saved.getAffectedUsers());
    }

    @Test
    void emptyTitleIsRejectedAndNothingChanges() {
        assertThrows(InvalidIncidentException.class, () ->
                service.updateIncident(incident.getId(), "Asha", "  ", null, null, null, 50));

        Incident saved = service.getIncident(incident.getId());
        assertEquals("WiFi slow", saved.getTitle());
        assertEquals(3, saved.getAffectedUsers());
        assertTrue(recorder.events.isEmpty());
    }

    @Test
    void zeroAffectedUsersIsRejected() {
        assertThrows(InvalidIncidentException.class, () ->
                service.updateIncident(incident.getId(), "Asha", null, null, null, null, 0));
    }

    @Test
    void cannotEditAResolvedIncident() {
        service.autoAssign(incident.getId(), "Admin");
        service.acknowledge(incident.getId(), "Ravi");
        service.startWork(incident.getId(), "Ravi");
        service.resolve(incident.getId(), "Ravi", "Restarted the access point");
        assertEquals(IncidentStatus.RESOLVED, service.getIncident(incident.getId()).getStatus());

        assertThrows(InvalidStateTransitionException.class, () ->
                service.updateIncident(incident.getId(), "Asha", "Changed", null, null, null, null));
    }

    @Test
    void canEditWhileInProgress() {
        service.autoAssign(incident.getId(), "Admin");
        service.acknowledge(incident.getId(), "Ravi");
        service.startWork(incident.getId(), "Ravi");

        Incident updated = service.updateIncident(incident.getId(), "Asha",
                null, null, null, null, 25);

        assertEquals(IncidentStatus.IN_PROGRESS, updated.getStatus());
        assertEquals(Priority.P1, updated.getPriority());
    }
}
