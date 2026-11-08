package edu.bits.itsm.domain;

public class ClassifiedState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.CLASSIFIED;
    }

    @Override
    public IncidentState assign() {
        return new AssignedState();
    }
}