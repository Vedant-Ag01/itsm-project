package edu.bits.itsm.domain;

public class ClosedState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.CLOSED;
    }
    // No actions allowed: a closed incident is final.
}