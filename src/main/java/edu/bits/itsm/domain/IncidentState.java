package edu.bits.itsm.domain;

public interface IncidentState {

    // The status this state represents (NEW, CLASSIFIED, ...).
    IncidentStatus getStatus();

    // Each action returns the NEXT state if it is legal from here.
    // By default every action is illegal; a state overrides only its own allowed action.
    default IncidentState classify() {
        throw illegal("classify");
    }

    default IncidentState assign() {
        throw illegal("assign");
    }

    default IncidentState acknowledge() {
        throw illegal("acknowledge");
    }

    default IncidentState startWork() {
        throw illegal("start work on");
    }

    default IncidentState resolve() {
        throw illegal("resolve");
    }

    default IncidentState verify() {
        throw illegal("verify");
    }

    default IncidentState close() {
        throw illegal("close");
    }

    private InvalidStateTransitionException illegal(String action) {
        return new InvalidStateTransitionException(getStatus(), action);
    }
}