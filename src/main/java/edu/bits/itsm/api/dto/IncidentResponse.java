package edu.bits.itsm.api.dto;

import edu.bits.itsm.domain.Incident;

import java.time.LocalDateTime;

public record IncidentResponse(
        String id,
        String title,
        String description,
        String status,
        String priority,
        String category,
        String serviceName,
        String reporterName,
        String assignedTechnician,
        int affectedUsers,
        LocalDateTime createdAt) {

    // Turns a domain Incident into the shape we send out
    public static IncidentResponse from(Incident incident) {
        String priority = null;
        if (incident.getPriority() != null) {
            priority = incident.getPriority().name();
        }

        String category = null;
        if (incident.getCategory() != null) {
            category = incident.getCategory().name();
        }

        String technician = null;
        if (incident.getAssignedTechnician() != null) {
            technician = incident.getAssignedTechnician().getName();
        }

        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus().name(),
                priority,
                category,
                incident.getService().getName(),
                incident.getReporter().getName(),
                technician,
                incident.getAffectedUsers(),
                incident.getCreatedAt());
    }
}