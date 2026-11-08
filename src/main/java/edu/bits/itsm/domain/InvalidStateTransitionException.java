package edu.bits.itsm.domain;

public class InvalidStateTransitionException extends RuntimeException {

    private final IncidentStatus currentStatus;
    private final String attemptedAction;

    public InvalidStateTransitionException(IncidentStatus currentStatus, String attemptedAction) {
        super("Cannot " + attemptedAction + " an incident that is " + currentStatus);
        this.currentStatus = currentStatus;
        this.attemptedAction = attemptedAction;
    }

    public IncidentStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }
}