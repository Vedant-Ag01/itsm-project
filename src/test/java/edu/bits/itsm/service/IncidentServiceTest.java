package edu.bits.itsm.service;

import edu.bits.itsm.assignment.AssignmentStrategy;
import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentNotFoundException;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.InvalidStateTransitionException;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.TechnicianUnavailableException;
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

class IncidentServiceTest {

    // ---------- simple stand-ins for B's and C's real classes ----------

    // always says P1
    private static class FixedPriorityStrategy implements PriorityStrategy {
        public Priority calculate(Impact impact, Urgency urgency,
                                  BusinessCriticality criticality, int affectedUsers) {
            return Priority.P1;
        }

        public String getName() {
            return "Fixed";
        }
    }

    // takes the first technician in the list
    private static class FirstTechnicianStrategy implements AssignmentStrategy {
        public Optional<Technician> selectTechnician(Incident incident,
                                                     List<Technician> candidates,
                                                     Map<Technician, Integer> activeLoad) {
            if (candidates.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(candidates.get(0));
        }

        public String getName() {
            return "First technician";
        }
    }

    // never finds anyone
    private static class NobodyStrategy implements AssignmentStrategy {
        public Optional<Technician> selectTechnician(Incident incident,
                                                     List<Technician> candidates,
                                                     Map<Technician, Integer> activeLoad) {
            return Optional.empty();
        }

        public String getName() {
            return "Nobody";
        }
    }

    // never finds duplicates
    private static class NoDuplicatesDetector implements DuplicateDetector {
        public List<DuplicateMatch> findSimilar(Incident incoming, List<Incident> existing) {
            return new ArrayList<>();
        }
    }

    // says two incidents are duplicates when their titles are exactly the same
    private static class SameTitleDetector implements DuplicateDetector {
        public List<DuplicateMatch> findSimilar(Incident incoming, List<Incident> existing) {
            List<DuplicateMatch> matches = new ArrayList<>();
            for (Incident other : existing) {
                if (other.getTitle().equals(incoming.getTitle())) {
                    matches.add(new DuplicateMatch(other, 1.0));
                }
            }
            return matches;
        }
    }

    // remembers every event it hears
    private static class RecordingObserver implements IncidentObserver {
        final List<IncidentEvent> events = new ArrayList<>();

        public void onEvent(IncidentEvent event) {
            events.add(event);
        }
    }

    // ---------- setup ----------

    private IncidentRepository repository;
    private RecordingObserver observer;
    private Technician ravi;
    private List<Technician> technicians;
    private IncidentService service;

    private Employee asha;
    private ItService payment;

    @BeforeEach
    void setUp() {
        repository = new InMemoryIncidentRepository();
        observer = new RecordingObserver();
        ravi = new Technician("T1", "Ravi", "ravi@bits.edu");
        technicians = new ArrayList<>();
        technicians.add(ravi);
        service = buildService(new NoDuplicatesDetector());

        // If this line is red, match it to your Employee constructor
        asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        payment = new ItService("Payment application", BusinessCriticality.HIGH);
    }

    private IncidentService buildService(DuplicateDetector detector) {
        List<IncidentObserver> observers = new ArrayList<>();
        observers.add(observer);
        return new IncidentService(repository, new IncidentFactory(repository),
                new FixedPriorityStrategy(), new FirstTechnicianStrategy(),
                detector, observers, technicians);
    }

    private Incident report(String title) {
        CreationResult result = service.createIncident(asha, title, "Payments are failing",
                payment, Impact.HIGH, Urgency.HIGH, 50, IncidentCategory.APPLICATION);
        return result.incident();
    }

    // ---------- creating ----------

    @Test
    void newIncidentIsClassifiedPrioritizedAndSaved() {
        Incident incident = report("Payment down");

        assertEquals(IncidentStatus.CLASSIFIED, incident.getStatus());
        assertEquals(Priority.P1, incident.getPriority());
        assertEquals(IncidentCategory.APPLICATION, incident.getCategory());
        assertTrue(repository.findById(incident.getId()).isPresent());
    }

    @Test
    void creatingPublishesEventsInOrder() {
        report("Payment down");

        assertEquals(3, observer.events.size());
        assertEquals(IncidentEventType.CREATED, observer.events.get(0).type());
        assertEquals(IncidentEventType.CLASSIFIED, observer.events.get(1).type());
        assertEquals(IncidentEventType.PRIORITY_CALCULATED, observer.events.get(2).type());
    }

    @Test
    void duplicateIsFlaggedWhenTheDetectorFindsOne() {
        IncidentService withDetector = buildService(new SameTitleDetector());
        withDetector.createIncident(asha, "Payment down", "Payments are failing",
                payment, Impact.HIGH, Urgency.HIGH, 50, IncidentCategory.APPLICATION);

        CreationResult second = withDetector.createIncident(asha, "Payment down",
                "Payments are failing again", payment, Impact.HIGH, Urgency.HIGH, 20,
                IncidentCategory.APPLICATION);

        assertEquals(1, second.possibleDuplicates().size());
        boolean flagged = false;
        for (IncidentEvent event : observer.events) {
            if (event.type() == IncidentEventType.DUPLICATE_FLAGGED) {
                flagged = true;
            }
        }
        assertTrue(flagged);
    }

    // ---------- assigning ----------

    @Test
    void autoAssignGivesTheIncidentToAnAvailableTechnician() {
        Incident incident = report("Payment down");

        service.autoAssign(incident.getId(), "System");

        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus());
        assertEquals("Ravi", incident.getAssignedTechnician().getName());
    }

    @Test
    void autoAssignFailsWhenNobodyIsAvailable() {
        Incident incident = report("Payment down");
        ravi.setAvailable(false);

        assertThrows(TechnicianUnavailableException.class,
                () -> service.autoAssign(incident.getId(), "System"));
        assertEquals(IncidentStatus.CLASSIFIED, incident.getStatus());
    }

    @Test
    void changingTheStrategyChangesWhoGetsPicked() {
        Incident incident = report("Payment down");
        service.changeAssignmentStrategy(new NobodyStrategy());

        assertThrows(TechnicianUnavailableException.class,
                () -> service.autoAssign(incident.getId(), "System"));
    }

    // ---------- the whole flow ----------

    @Test
    void incidentGoesFromReportToClosedAndEveryStepIsAnnounced() {
        Incident incident = report("Payment down");
        String id = incident.getId();

        service.autoAssign(id, "System");
        service.acknowledge(id, "Ravi");
        service.startWork(id, "Ravi");
        service.resolve(id, "Ravi", "Restarted the payment service");
        service.verify(id, "Asha");
        service.close(id, "Admin", "Employee confirmed the fix");

        assertEquals(IncidentStatus.CLOSED, service.getIncident(id).getStatus());

        List<IncidentEventType> types = new ArrayList<>();
        for (IncidentEvent event : observer.events) {
            types.add(event.type());
        }
        assertEquals(List.of(
                IncidentEventType.CREATED,
                IncidentEventType.CLASSIFIED,
                IncidentEventType.PRIORITY_CALCULATED,
                IncidentEventType.ASSIGNED,
                IncidentEventType.ACKNOWLEDGED,
                IncidentEventType.WORK_STARTED,
                IncidentEventType.RESOLVED,
                IncidentEventType.VERIFIED,
                IncidentEventType.CLOSED), types);
    }

    // ---------- things that must fail ----------

    @Test
    void illegalStepIsRejectedAndNothingIsAnnounced() {
        Incident incident = report("Payment down");
        int eventsBefore = observer.events.size();

        assertThrows(InvalidStateTransitionException.class,
                () -> service.close(incident.getId(), "Admin", "too early"));

        assertEquals(IncidentStatus.CLASSIFIED, incident.getStatus());
        assertEquals(eventsBefore, observer.events.size());
    }

    @Test
    void unknownIncidentIdGivesNotFound() {
        assertThrows(IncidentNotFoundException.class,
                () -> service.getIncident("INC9999"));
    }
}