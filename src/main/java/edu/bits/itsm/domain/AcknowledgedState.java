package edu.bits.itsm.domain;

public class AcknowledgedState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.ACKNOWLEDGED;
    }

    @Override
    public IncidentState startWork() {
        return new InProgressState();
    }
}