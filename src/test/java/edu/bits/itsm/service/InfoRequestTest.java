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
import edu.bits.itsm.domain.NoteType;
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

class InfoRequestTest {

    private static class FixedPriority implements PriorityStrategy {
        public Priority calculate(Impact impact, Urgency urgency,
                                  BusinessCriticality criticality, int affectedUsers) {
            return Priority.P3;
        }

        public String getName() {
            return "Fixed";
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
    private Technician ravi;
    private Employee asha;
    private Incident incident;

    @BeforeEach
    void setUp() {
        IncidentRepository repository = new InMemoryIncidentRepository();
        recorder = new Recorder();
        List<IncidentObserver> observers = new ArrayList<>();
        observers.add(recorder);
        ravi = new Technician("T1", "Ravi", "ravi@bits.edu");
        List<Technician> technicians = new ArrayList<>();
        technicians.add(ravi);
        service = new IncidentService(repository, new IncidentFactory(repository),
                new FixedPriority(), new FirstTechnician(), new NoDuplicates(),
                observers, technicians);
        asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        incident = service.createIncident(asha, "VPN fails", "VPN will not connect",
                new ItService("VPN Gateway", BusinessCriticality.HIGH), Impact.MEDIUM,
                Urgency.MEDIUM, 4, IncidentCategory.NETWORK).incident();
        service.autoAssign(incident.getId(), "Admin");
        recorder.events.clear();
    }

    @Test
    void requestingInformationMarksTheIncidentAsAwaitingAndKeepsItsStatus() {
        IncidentStatus before = service.getIncident(incident.getId()).getStatus();

        service.requestInformation(incident.getId(), ravi, "Which error code do you see?");

        Incident saved = service.getIncident(incident.getId());
        assertTrue(saved.isAwaitingInfo());
        assertEquals(before, saved.getStatus());
        assertEquals(NoteType.INFO_REQUEST,
                saved.getInvestigationNotes().get(saved.getInvestigationNotes().size() - 1).getType());
        assertEquals(IncidentEventType.INFO_REQUESTED, recorder.events.get(0).type());
    }

    @Test
    void answeringClearsTheAwaitingFlag() {
        service.requestInformation(incident.getId(), ravi, "Which error code do you see?");

        service.provideInformation(incident.getId(), asha, "Error 809");

        assertFalse(service.getIncident(incident.getId()).isAwaitingInfo());
        assertEquals(IncidentEventType.INFO_PROVIDED, recorder.events.get(1).type());
    }

    @Test
    void aSecondRequestMakesItAwaitingAgain() {
        service.requestInformation(incident.getId(), ravi, "Which error code?");
        service.provideInformation(incident.getId(), asha, "Error 809");
        service.requestInformation(incident.getId(), ravi, "Does it happen on campus WiFi too?");

        assertTrue(service.getIncident(incident.getId()).isAwaitingInfo());
    }

    @Test
    void cannotAnswerWhenNothingWasAsked() {
        assertThrows(InvalidIncidentException.class,
                () -> service.provideInformation(incident.getId(), asha, "Unasked answer"));
    }

    @Test
    void cannotRequestInformationOnAResolvedIncident() {
        service.acknowledge(incident.getId(), "Ravi");
        service.startWork(incident.getId(), "Ravi");
        service.resolve(incident.getId(), "Ravi", "Reset the VPN profile");

        assertThrows(InvalidStateTransitionException.class,
                () -> service.requestInformation(incident.getId(), ravi, "Anything else?"));
    }

    @Test
    void emptyMessageIsRejected() {
        assertThrows(InvalidIncidentException.class,
                () -> service.requestInformation(incident.getId(), ravi, "   "));
    }
}
