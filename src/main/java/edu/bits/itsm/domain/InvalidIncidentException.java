package edu.bits.itsm.domain;

// Thrown when someone tries to create or fill an incident with bad data.
// It extends IllegalArgumentException so existing checks and tests keep working,
// but it gives the project its own named error to catch and report.
public class InvalidIncidentException extends IllegalArgumentException {

    public InvalidIncidentException(String message) {
        super(message);
    }
}