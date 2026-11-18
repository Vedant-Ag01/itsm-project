package edu.bits.itsm.service;

import edu.bits.itsm.assignment.AssignmentStrategy;
import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.TechnicianOverloadedException;
import edu.bits.itsm.domain.TechnicianUnavailableException;
import edu.bits.itsm.domain.Urgency;
import edu.bits.itsm.duplicate.DuplicateDetector;
import edu.bits.itsm.duplicate.DuplicateMatch;
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

class IncidentWorkloadTest {

    private static class FixedPriority implements PriorityStrategy {
        public Priority calculate(Impact impact, Urgency urgency,
                                  BusinessCriticality criticality, int affectedUsers) {
            return Priority.P2;
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

    private IncidentService service;
    private Technician ravi;
    private Employee asha;
    private ItService wifi;

    @BeforeEach
    void setUp() {
        IncidentRepository repository = new InMemoryIncidentRepository();
        ravi = new Technician("T1", "Ravi", "ravi@bits.edu");
        List<Technician> technicians = new ArrayList<>();
        technicians.add(ravi);
        service = new IncidentService(repository, new IncidentFactory(repository),
                new FixedPriority(), new FirstTechnician(), new NoDuplicates(),
                new ArrayList<IncidentObserver>(), technicians);
        asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        wifi = new ItService("Campus WiFi", BusinessCriticality.HIGH);
    }

    private Incident report(String title) {
        return service.createIncident(asha, title, "Something is broken", wifi,
                Impact.HIGH, Urgency.HIGH, 5, IncidentCategory.NETWORK).incident();
    }

    @Test
    void defaultLimitIsEight() {
        assertEquals(8, service.getMaxActivePerTechnician());
    }

    @Test
    void autoAssignSkipsATechnicianWhoIsAtTheLimit() {
        service.setMaxActivePerTechnician(1);
        Incident first = report("First problem");
        Incident second = report("Second problem");

        service.autoAssign(first.getId(), "Admin");

        assertThrows(TechnicianUnavailableException.class,
                () -> service.autoAssign(second.getId(), "Admin"));
    }

    @Test
    void manualAssignmentBeyondTheLimitIsRefused() {
        service.setMaxActivePerTechnician(1);
        Incident first = report("First problem");
        Incident second = report("Second problem");
        service.assignTo(first.getId(), ravi, "Admin");

        TechnicianOverloadedException e = assertThrows(TechnicianOverloadedException.class,
                () -> service.assignTo(second.getId(), ravi, "Admin"));
        assertTrue(e.getMessage().contains("workload exceeded"));
    }

    @Test
    void finishingAnIncidentFreesCapacity() {
        service.setMaxActivePerTechnician(1);
        Incident first = report("First problem");
        Incident second = report("Second problem");
        service.assignTo(first.getId(), ravi, "Admin");
        assertEquals(1, service.activeLoadOf(ravi));

        service.acknowledge(first.getId(), "Ravi");
        service.startWork(first.getId(), "Ravi");
        service.resolve(first.getId(), "Ravi", "Rebooted the switch");

        assertEquals(0, service.activeLoadOf(ravi));
        assertDoesNotThrow(() -> service.assignTo(second.getId(), ravi, "Admin"));
    }

    @Test
    void raisingTheLimitAllowsMoreWork() {
        service.setMaxActivePerTechnician(1);
        Incident first = report("First problem");
        Incident second = report("Second problem");
        service.assignTo(first.getId(), ravi, "Admin");

        service.setMaxActivePerTechnician(2);

        assertDoesNotThrow(() -> service.assignTo(second.getId(), ravi, "Admin"));
        assertEquals(2, service.activeLoadOf(ravi));
    }

    @Test
    void aLimitBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.setMaxActivePerTechnician(0));
    }
}
