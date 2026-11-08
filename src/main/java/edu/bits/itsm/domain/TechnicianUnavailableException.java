package edu.bits.itsm.domain;

// Thrown when the assignment strategy finds nobody to take an incident.
public class TechnicianUnavailableException extends RuntimeException {

    public TechnicianUnavailableException(String incidentId) {
        super("No technician is available for incident " + incidentId);
    }
}