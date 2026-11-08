package edu.bits.itsm.domain;

public class NewState implements IncidentState {

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.NEW;
    }

    @Override
    public IncidentState classify() {
        return new ClassifiedState();
    }
}