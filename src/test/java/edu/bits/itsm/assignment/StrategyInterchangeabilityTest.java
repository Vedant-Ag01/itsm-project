package edu.bits.itsm.assignment;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.TechnicianUnavailableException;
import edu.bits.itsm.domain.Urgency;
import edu.bits.itsm.priority.PriorityStrategy;
import edu.bits.itsm.repository.InMemoryIncidentRepository;
import edu.bits.itsm.repository.IncidentRepository;
import edu.bits.itsm.service.IncidentFactory;
import edu.bits.itsm.service.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Shows that the two strategies can be swapped behind the same interface.
class StrategyInterchangeabilityTest {

    private Technician a;
    private Technician b;
    private Technician c;
    private Technician d;

    @BeforeEach
    void setUp() {
        a = new Technician("T1", "A", "a@bits.edu");
        a.addSkill(IncidentCategory.NETWORK);
        b = new Technician("T2", "B", "b@bits.edu");
        b.addSkill(IncidentCategory.SOFTWARE);
        c = new Technician("T3", "C", "c@bits.edu");
        c.addSkill(IncidentCategory.NETWORK);
        d = new Technician("T4", "D", "d@bits.edu");
        d.addSkill(IncidentCategory.NETWORK);
        d.setAvailable(false);
    }

    private final Employee asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private final ItService vpn = new ItService("VPN", BusinessCriticality.HIGH);

    @Test
    void sameCallGivesDifferentTechnicianWhenTheStrategyIsSwapped() {
        Incident incident = new Incident("INC1001", asha, "VPN down", "Cannot connect to VPN",
                vpn, Impact.HIGH, Urgency.HIGH, 10);
        incident.setCategory(IncidentCategory.NETWORK);
        List<Technician> candidates = List.of(a, b, c, d);
        Map<Technician, Integer> load = new HashMap<>();
        load.put(a, 5);
        load.put(b, 0);
        load.put(c, 2);
        load.put(d, 0);

        AssignmentStrategy strategy = new SkillBasedStrategy();
        assertSame(c, strategy.selectTechnician(incident, candidates, load).orElseThrow());

        strategy = new LoadBasedStrategy();
        assertSame(b, strategy.selectTechnician(incident, candidates, load).orElseThrow());
    }

    @Test
    void incidentServiceUsesWhicheverRealStrategyIsPluggedIn() {
        IncidentRepository repository = new InMemoryIncidentRepository();
        List<Technician> technicians = new ArrayList<>(List.of(b, a, c, d));
        IncidentService service = new IncidentService(repository, new IncidentFactory(repository),
                new FixedPriority(),
                new LoadBasedStrategy(),
                (incoming, existing) -> new ArrayList<>(),              // no duplicates
                new ArrayList<>(),
                technicians);
        Incident first = report(service, IncidentCategory.NETWORK);
        Incident second = report(service, IncidentCategory.NETWORK);
        Incident database = report(service, IncidentCategory.DATABASE);

        // everyone at 0, D unavailable: load-based takes B, the first in the list
        service.autoAssign(first.getId(), "System");
        assertSame(b, first.getAssignedTechnician());

        // skill-based skips B (no NETWORK skill); A and C are 0, A comes first
        service.changeAssignmentStrategy(new SkillBasedStrategy());
        service.autoAssign(second.getId(), "System");
        assertSame(a, second.getAssignedTechnician());

        // no DATABASE skill anywhere: empty selection becomes the service's exception
        assertThrows(TechnicianUnavailableException.class,
                () -> service.autoAssign(database.getId(), "System"));
        assertNull(database.getAssignedTechnician());
    }

    // simple stand-in so the test does not depend on B2's priority classes
    private static class FixedPriority implements PriorityStrategy {
        @Override
        public Priority calculate(Impact impact, Urgency urgency,
                                  BusinessCriticality criticality, int affectedUsers) {
            return Priority.P2;
        }

        @Override
        public String getName() {
            return "Fixed";
        }
    }

    private Incident report(IncidentService service, IncidentCategory category) {
        return service.createIncident(asha, "VPN down", "Cannot connect to VPN", vpn,
                Impact.HIGH, Urgency.HIGH, 10, category).incident();
    }
}
