package edu.bits.itsm.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class IncidentRestoreTest {

    private Incident restoreWithStatus(IncidentStatus status, LocalDateTime createdAt) {
        // If this line is red, match it to your Employee constructor (same as the other tests)
        Employee asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        ItService payment = new ItService("Payment application", BusinessCriticality.HIGH);
        Technician ravi = new Technician("T1", "Ravi", "ravi@bits.edu");

        return Incident.restore("INC1001", asha, "Payment down", "Payments are failing",
                payment, Impact.HIGH, Urgency.HIGH, 50, createdAt, status,
                IncidentCategory.APPLICATION, Priority.P1, ravi,
                null, null, null, null, null, new ArrayList<>(), null);
    }

    @Test
    void restoredIncidentKeepsItsStatusAndCreationTime() {
        LocalDateTime created = LocalDateTime.of(2026, 11, 1, 9, 30);

        Incident incident = restoreWithStatus(IncidentStatus.RESOLVED, created);

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertEquals(created, incident.getCreatedAt());
        assertEquals("Ravi", incident.getAssignedTechnician().getName());
    }

    @Test
    void restoredIncidentCanContinueFromWhereItStopped() {
        Incident incident = restoreWithStatus(IncidentStatus.RESOLVED, LocalDateTime.now());

        incident.verify();

        assertEquals(IncidentStatus.VERIFIED, incident.getStatus());
    }

    @Test
    void restoredIncidentStillRefusesIllegalMoves() {
        Incident incident = restoreWithStatus(IncidentStatus.CLOSED, LocalDateTime.now());

        assertThrows(InvalidStateTransitionException.class, incident::acknowledge);
    }
}