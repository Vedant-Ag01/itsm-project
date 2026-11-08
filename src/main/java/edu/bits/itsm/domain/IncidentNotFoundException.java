package edu.bits.itsm.domain;

// Thrown when someone asks for an incident id that does not exist.
public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(String id) {
        super("No incident found with id " + id);
    }
}