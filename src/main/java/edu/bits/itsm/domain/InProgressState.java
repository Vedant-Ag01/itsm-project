package edu.bits.itsm.domain;

public class InProgressState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.IN_PROGRESS;
    }

    @Override
    public IncidentState resolve() {
        return new ResolvedState();
    }
}