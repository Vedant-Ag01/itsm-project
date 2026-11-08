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

    private ItService service() {
        return new ItService("Payment application", BusinessCriticality.HIGH);
    }

    private Incident newIncident() {
        return new Incident("INC1", employee(), "Payment down",
                "Payment application is unavailable",
                service(), Impact.HIGH, Urgency.HIGH, 120);
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
        incident.resolve("Restarted the payment service");
        if (target == IncidentStatus.RESOLVED) return incident;
        incident.verify();
        if (target == IncidentStatus.VERIFIED) return incident;
        incident.close("Employee confirmed the fix");
        return incident;
    }

    // ---------- creation and validation ----------

    @Test
    void newIncidentStartsAsNew() {
        assertEquals(IncidentStatus.NEW, newIncident().getStatus());
    }

    @Test
    void newIncidentStoresItsDetails() {
        Incident incident = newIncident();
        assertEquals("Payment down", incident.getTitle());
        assertEquals("Payment application", incident.getService().getName());
        assertEquals(Impact.HIGH, incident.getImpact());
        assertEquals(Urgency.HIGH, incident.getUrgency());
        assertEquals(120, incident.getAffectedUsers());
    }

    @Test
    void incidentWithBlankTitleIsRejected() {
        assertThrows(InvalidIncidentException.class, () ->
                new Incident("INC2", employee(), " ", "desc",
                        service(), Impact.LOW, Urgency.LOW, 1));
    }

    @Test
    void incidentWithoutServiceIsRejected() {
        assertThrows(InvalidIncidentException.class, () ->
                new Incident("INC2", employee(), "title", "desc",
                        null, Impact.LOW, Urgency.LOW, 1));
    }

    @Test
    void incidentWithZeroAffectedUsersIsRejected() {
        assertThrows(InvalidIncidentException.class, () ->
                new Incident("INC2", employee(), "title", "desc",
                        service(), Impact.LOW, Urgency.LOW, 0));
    }

    // ---------- legal moves ----------

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

        incident.resolve("Restarted the payment service");
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());

        incident.verify();
        assertEquals(IncidentStatus.VERIFIED, incident.getStatus());

        incident.close("Employee confirmed the fix");
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
        incident.resolve("Restarted the payment service");
        assertNotNull(incident.getResolvedAt());

        incident.verify();
        assertNull(incident.getClosedAt());
        incident.close("Employee confirmed the fix");
        assertNotNull(incident.getClosedAt());
    }

    @Test
    void resolutionAndClosureNotesAreStored() {
        Incident incident = incidentAt(IncidentStatus.CLOSED);
        assertEquals("Restarted the payment service", incident.getResolutionNote());
        assertEquals("Employee confirmed the fix", incident.getClosureNote());
    }

    // ---------- illegal jumps ----------

    @Test
    void newIncidentCannotBeClosed() {
        Incident incident = newIncident();
        assertThrows(InvalidStateTransitionException.class, () -> incident.close("done"));
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
        assertThrows(InvalidStateTransitionException.class, () -> incident.close("done"));
    }

    @Test
    void closedIncidentAllowsNoFurtherActions() {
        Incident incident = incidentAt(IncidentStatus.CLOSED);
        assertThrows(InvalidStateTransitionException.class, incident::classify);
        assertThrows(InvalidStateTransitionException.class, () -> incident.assign(technician()));
        assertThrows(InvalidStateTransitionException.class, incident::acknowledge);
        assertThrows(InvalidStateTransitionException.class, incident::startWork);
        assertThrows(InvalidStateTransitionException.class, () -> incident.resolve("again"));
        assertThrows(InvalidStateTransitionException.class, incident::verify);
        assertThrows(InvalidStateTransitionException.class, () -> incident.close("again"));
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
        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class, () -> incident.close("done"));
        assertEquals(IncidentStatus.NEW, ex.getCurrentStatus());
        assertTrue(ex.getMessage().contains("close"));
        assertTrue(ex.getMessage().contains("NEW"));
    }

    @Test
    void assigningToNoTechnicianIsRejected() {
        Incident incident = incidentAt(IncidentStatus.CLASSIFIED);
        assertThrows(IllegalArgumentException.class, () -> incident.assign(null));
    }

    // ---------- resolution, notes and feedback ----------

    @Test
    void resolvingNeedsResolutionDetails() {
        Incident incident = incidentAt(IncidentStatus.IN_PROGRESS);
        assertThrows(InvalidIncidentException.class, () -> incident.resolve(" "));
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
    }

    @Test
    void investigationNotesAreStoredInOrder() {
        Incident incident = incidentAt(IncidentStatus.IN_PROGRESS);
        incident.addInvestigationNote(new InvestigationNote(technician(), "Checked the logs"));
        incident.addInvestigationNote(new InvestigationNote(technician(), "Database was full"));
        assertEquals(2, incident.getInvestigationNotes().size());
        assertEquals("Checked the logs", incident.getInvestigationNotes().get(0).getText());
    }

    @Test
    void notesListCannotBeChangedFromOutside() {
        Incident incident = newIncident();
        assertThrows(UnsupportedOperationException.class, () ->
                incident.getInvestigationNotes()
                        .add(new InvestigationNote(technician(), "sneaky")));
    }

    @Test
    void notesCannotBeAddedToAClosedIncident() {
        Incident incident = incidentAt(IncidentStatus.CLOSED);
        assertThrows(InvalidStateTransitionException.class, () ->
                incident.addInvestigationNote(new InvestigationNote(technician(), "late note")));
    }

    @Test
    void feedbackIsOnlyAllowedAfterResolution() {
        Incident early = incidentAt(IncidentStatus.IN_PROGRESS);
        assertThrows(InvalidStateTransitionException.class,
                () -> early.giveFeedback(new Feedback(5, "great")));

        Incident resolved = incidentAt(IncidentStatus.RESOLVED);
        resolved.giveFeedback(new Feedback(5, "great"));
        assertEquals(5, resolved.getFeedback().getRating());
    }

    @Test
    void feedbackRatingMustBeBetweenOneAndFive() {
        assertThrows(InvalidIncidentException.class, () -> new Feedback(0, "bad"));
        assertThrows(InvalidIncidentException.class, () -> new Feedback(6, "wow"));
    }
}