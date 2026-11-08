package edu.bits.itsm.domain;

public class AssignedState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.ASSIGNED;
    }

    @Override
    public IncidentState acknowledge() {
        return new AcknowledgedState();
    }
}