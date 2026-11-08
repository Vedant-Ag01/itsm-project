package edu.bits.itsm.service;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.InvalidIncidentException;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Urgency;
import edu.bits.itsm.repository.InMemoryIncidentRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentFactoryTest {

    private final Employee asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private final ItService payment = new ItService("Payment application", BusinessCriticality.HIGH);

    @Test
    void createdIncidentGetsTheNextIdAndStartsAsNew() {
        IncidentFactory factory = new IncidentFactory(new InMemoryIncidentRepository());

        Incident incident = factory.create(asha, "Payment down", "Payments are failing",
                payment, Impact.HIGH, Urgency.HIGH, 50);

        assertEquals("INC1001", incident.getId());
        assertEquals(IncidentStatus.NEW, incident.getStatus());
    }

    @Test
    void twoIncidentsGetDifferentIds() {
        IncidentFactory factory = new IncidentFactory(new InMemoryIncidentRepository());

        Incident first = factory.create(asha, "Payment down", "Payments are failing",
                payment, Impact.HIGH, Urgency.HIGH, 50);
        Incident second = factory.create(asha, "Slow checkout", "Checkout is slow",
                payment, Impact.MEDIUM, Urgency.LOW, 5);

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void badInputIsRejected() {
        IncidentFactory factory = new IncidentFactory(new InMemoryIncidentRepository());

        assertThrows(InvalidIncidentException.class, () ->
                factory.create(asha, "", "Payments are failing",
                        payment, Impact.HIGH, Urgency.HIGH, 50));
    }
}