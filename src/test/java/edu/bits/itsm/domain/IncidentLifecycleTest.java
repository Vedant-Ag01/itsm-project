package edu.bits.itsm.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncidentLifecycleTest {

    // ---------- helpers ----------

    // If this line is red, open Employee.java, look at its constructor,
    // and adjust the arguments here. This is the only place that creates employees.
    private Employee employee() {
        return new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    }

    private Technician technician() {
        return new Technician("T1", "Ravi", "ravi@bits.edu");
    }

    private Incident newIncident() {
        return new Incident("INC1", employee(), "Payment application is unavailable");
    }

    // Builds an incident and moves it forward until it reaches the wanted status.
    private Incident incidentAt(IncidentStatus target) {
        Incident incident = newIncident();
        if (target == IncidentStatus.NEW) return incident;
        incident.classify();
        if (target == IncidentStatus.CLASSIFIED) return incident;
        incident.assign(technician());
        if (target == IncidentStatus.ASSIGNED) return incident;
        incident.acknowledge();
        if (target == IncidentStatus.ACKNOWLEDGED) return incident;
        incident.startWork();
        if (target == IncidentStatus.IN_PROGRESS) return incident;
        incident.resolve();
        if (target == IncidentStatus.RESOLVED) return incident;
        incident.verify();
        if (target == IncidentStatus.VERIFIED) return incident;
        incident.close();
        return incident;
    }

    // ---------- legal moves ----------

    @Test
    void newIncidentStartsAsNew() {
        assertEquals(IncidentStatus.NEW, newIncident().getStatus());
    }

    @Test
    void fullLifecycleWalksFromNewToClosed() {
        Incident incident = newIncident();

        incident.classify();
        assertEquals(IncidentStatus.CLASSIFIED, incident.getStatus());

        incident.assign(technician());
        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus());

        incident.acknowledge();
        assertEquals(IncidentStatus.ACKNOWLEDGED, incident.getStatus());

        incident.startWork();
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());

        incident.resolve();
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());

        incident.verify();
        assertEquals(IncidentStatus.VERIFIED, incident.getStatus());

        incident.close();
        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
    }

    @Test
    void assignStoresTheTechnician() {
        Incident incident = incidentAt(IncidentStatus.ASSIGNED);
        assertNotNull(incident.getAssignedTechnician());
        assertEquals("Ravi", incident.getAssignedTechnician().getName());
    }

    @Test
    void timestampsAreFilledInAsTheIncidentProgresses() {
        Incident incident = incidentAt(IncidentStatus.ASSIGNED);
        assertNull(incident.getAcknowledgedAt());

        incident.acknowledge();
        assertNotNull(incident.getAcknowledgedAt());

        incident.startWork();
        assertNull(incident.getResolvedAt());
        incident.resolve();
        assertNotNull(incident.getResolvedAt());

        incident.verify();
        assertNull(incident.getClosedAt());
        incident.close();
        assertNotNull(incident.getClosedAt());
    }

    // ---------- illegal jumps ----------

    @Test
    void newIncidentCannotBeClosed() {
        Incident incident = newIncident();
        assertThrows(InvalidStateTransitionException.class, incident::close);
        assertEquals(IncidentStatus.NEW, incident.getStatus());
    }

    @Test
    void newIncidentCannotBeAssignedBeforeClassification() {
        Incident incident = newIncident();
        assertThrows(InvalidStateTransitionException.class,
                () -> incident.assign(technician()));
    }

    @Test
    void assignedIncidentCannotSkipAcknowledgement() {
        Incident incident = incidentAt(IncidentStatus.ASSIGNED);
        assertThrows(InvalidStateTransitionException.class, incident::startWork);
    }

    @Test
    void resolvedIncidentCannotBeClosedBeforeVerification() {
        Incident incident = incidentAt(IncidentStatus.RESOLVED);
        assertThrows(InvalidStateTransitionException.class, incident::close);
    }

    @Test
    void closedIncidentAllowsNoFurtherActions() {
        Incident incident = incidentAt(IncidentStatus.CLOSED);
        assertThrows(InvalidStateTransitionException.class, incident::classify);
        assertThrows(InvalidStateTransitionException.class, () -> incident.assign(technician()));
        assertThrows(InvalidStateTransitionException.class, incident::acknowledge);
        assertThrows(InvalidStateTransitionException.class, incident::startWork);
        assertThrows(InvalidStateTransitionException.class, incident::resolve);
        assertThrows(InvalidStateTransitionException.class, incident::verify);
        assertThrows(InvalidStateTransitionException.class, incident::close);
    }

    @Test
    void illegalAssignDoesNotStoreTheTechnician() {
        Incident incident = newIncident();
        assertThrows(InvalidStateTransitionException.class,
                () -> incident.assign(technician()));
        assertNull(incident.getAssignedTechnician());
    }

    @Test
    void exceptionMessageNamesTheStatusAndAction() {
        Incident incident = newIncident();
        InvalidStateTransitionException ex =
                assertThrows(InvalidStateTransitionException.class, incident::close);
        assertEquals(IncidentStatus.NEW, ex.getCurrentStatus());
        assertTrue(ex.getMessage().contains("close"));
        assertTrue(ex.getMessage().contains("NEW"));
    }

    @Test
    void assigningToNoTechnicianIsRejected() {
        Incident incident = incidentAt(IncidentStatus.CLASSIFIED);
        assertThrows(IllegalArgumentException.class, () -> incident.assign(null));
    }
}