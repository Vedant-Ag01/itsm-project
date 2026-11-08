package edu.bits.itsm.domain;

public class ResolvedState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.RESOLVED;
    }

    @Override
    public IncidentState verify() {
        return new VerifiedState();
    }
}