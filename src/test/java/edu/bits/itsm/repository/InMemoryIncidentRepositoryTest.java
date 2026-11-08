package edu.bits.itsm.repository;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Urgency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryIncidentRepositoryTest {

    private Incident makeIncident(String id) {
        Employee asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        ItService service = new ItService("Payment application", BusinessCriticality.HIGH);
        return new Incident(id, asha, "Payment down", "Payments are failing",
                service, Impact.HIGH, Urgency.HIGH, 10);
    }

    @Test
    void savedIncidentCanBeFoundById() {
        IncidentRepository repo = new InMemoryIncidentRepository();
        repo.save(makeIncident("INC1"));
        assertTrue(repo.findById("INC1").isPresent());
    }

    @Test
    void missingIncidentGivesEmpty() {
        IncidentRepository repo = new InMemoryIncidentRepository();
        assertTrue(repo.findById("NOPE").isEmpty());
    }

    @Test
    void findAllReturnsEverythingSaved() {
        IncidentRepository repo = new InMemoryIncidentRepository();
        repo.save(makeIncident("INC1"));
        repo.save(makeIncident("INC2"));
        assertEquals(2, repo.findAll().size());
    }

    @Test
    void nextIdCountsUp() {
        IncidentRepository repo = new InMemoryIncidentRepository();
        assertEquals("INC1001", repo.nextId());
        assertEquals("INC1002", repo.nextId());
    }
}