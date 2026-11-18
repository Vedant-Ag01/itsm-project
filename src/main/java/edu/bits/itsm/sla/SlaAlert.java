package edu.bits.itsm.sla;


public record SlaAlert(String incidentId, Type type, String message) {

    public enum Type { WARNING, BREACH }

    public SlaAlert {
        if (incidentId == null || incidentId.isBlank()) {
            throw new IllegalArgumentException("incidentId must not be empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be empty");
        }
    }
}
