package edu.bits.itsm.event;

public interface IncidentObserver {
    void onEvent(IncidentEvent event);
}