package edu.bits.itsm.notification;

import edu.bits.itsm.event.IncidentEvent;

public class NotificationFactory {

    public record NotificationContent(String subject, String body) { }

    public static NotificationContent createMessage(IncidentEvent event) {
        String subject = String.format("[%s] Incident Update: %s", event.type(), event.incidentId());
        String body = String.format("Incident %s has been updated by %s.\nEvent: %s\nStatus: %s -> %s\nDetails: %s",
                event.incidentId(),
                event.actor(),
                event.type(),
                event.previousStatus() != null ? event.previousStatus() : "NONE",
                event.newStatus(),
                event.description());

        return new NotificationContent(subject, body);
    }
}