package edu.bits.itsm.domain;

// Thrown when a technician already has as many active incidents as allowed
public class TechnicianOverloadedException extends RuntimeException {

    public TechnicianOverloadedException(String technicianName, int activeIncidents, int limit) {
        super("Technician workload exceeded: " + technicianName + " already has "
                + activeIncidents + " active incident(s) and the limit is " + limit);
    }
}
