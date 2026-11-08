package edu.bits.itsm.persistence;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Feedback;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.InvestigationNote;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.Urgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncidentMapperTest {

    private Technician ravi;
    private IncidentMapper mapper;
    private Employee asha;
    private ItService payment;

    @BeforeEach
    void setUp() {
        ravi = new Technician("T1", "Ravi", "ravi@bits.edu");
        List<Technician> technicians = new ArrayList<>();
        technicians.add(ravi);
        mapper = new IncidentMapper(technicians);

        asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        payment = new ItService("Payment application", BusinessCriticality.HIGH);
    }

    private Incident newIncident() {
        return new Incident("INC1001", asha, "Payment down", "Payments are failing",
                payment, Impact.HIGH, Urgency.HIGH, 50);
    }

    @Test
    void aBrandNewIncidentSurvivesTheRoundTrip() {
        Incident original = newIncident();

        Incident loaded = mapper.toIncident(mapper.toEntity(original));

        assertEquals("INC1001", loaded.getId());
        assertEquals(IncidentStatus.NEW, loaded.getStatus());
        assertEquals(original.getCreatedAt(), loaded.getCreatedAt());
        assertNull(loaded.getAssignedTechnician());
        assertNull(loaded.getFeedback());
        assertTrue(loaded.getInvestigationNotes().isEmpty());
    }

    @Test
    void aResolvedIncidentKeepsEverythingItHad() {
        Incident original = newIncident();
        original.classify();
        original.setCategory(IncidentCategory.APPLICATION);
        original.setPriority(Priority.P1);
        original.assign(ravi);
        original.acknowledge();
        original.startWork();
        original.addInvestigationNote(new InvestigationNote(ravi, "Checked the logs"));
        original.resolve("Restarted the payment service");
        original.giveFeedback(new Feedback(4, "Quick fix"));

        Incident loaded = mapper.toIncident(mapper.toEntity(original));

        assertEquals(IncidentStatus.RESOLVED, loaded.getStatus());
        assertEquals(Priority.P1, loaded.getPriority());
        assertEquals(IncidentCategory.APPLICATION, loaded.getCategory());
        assertSame(ravi, loaded.getAssignedTechnician());
        assertEquals("Finance", loaded.getReporter().getDepartment());
        assertEquals(BusinessCriticality.HIGH, loaded.getService().getCriticality());
        assertEquals(original.getAcknowledgedAt(), loaded.getAcknowledgedAt());
        assertEquals(original.getResolvedAt(), loaded.getResolvedAt());
        assertEquals("Restarted the payment service", loaded.getResolutionNote());
        assertEquals(4, loaded.getFeedback().getRating());
        assertEquals("Quick fix", loaded.getFeedback().getComment());

        assertEquals(1, loaded.getInvestigationNotes().size());
        InvestigationNote note = loaded.getInvestigationNotes().get(0);
        assertEquals("Checked the logs", note.getText());
        assertEquals("Ravi", note.getAuthor().getName());
        assertEquals(original.getInvestigationNotes().get(0).getCreatedAt(), note.getCreatedAt());
    }

    @Test
    void aLoadedIncidentCanContinueItsLifecycle() {
        Incident original = newIncident();
        original.classify();
        original.assign(ravi);

        Incident loaded = mapper.toIncident(mapper.toEntity(original));
        loaded.acknowledge();

        assertEquals(IncidentStatus.ACKNOWLEDGED, loaded.getStatus());
    }
}