package edu.bits.itsm.domain;

public class VerifiedState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.VERIFIED;
    }

    @Override
    public IncidentState close() {
        return new ClosedState();
    }
}