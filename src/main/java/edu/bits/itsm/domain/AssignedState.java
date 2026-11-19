package edu.bits.itsm.domain;

public class AssignedState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.ASSIGNED;
    }

    // An admin may hand it to someone else until the technician acknowledges it
    @Override
    public IncidentState assign() {
        return this;
    }

    @Override
    public IncidentState acknowledge() {
        return new AcknowledgedState();
    }
}