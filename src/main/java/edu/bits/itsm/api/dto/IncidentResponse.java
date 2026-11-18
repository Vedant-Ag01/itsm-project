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
        String impact,
        String urgency,
        String reporterName,
        String assignedTechnician,
        int affectedUsers,
        LocalDateTime createdAt,
        LocalDateTime acknowledgedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        String resolutionNote,
        String closureNote,
        Integer feedbackRating,
        String feedbackComment,
        boolean awaitingInfo) {

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

        Integer rating = null;
        String ratingComment = null;
        if (incident.getFeedback() != null) {
            rating = incident.getFeedback().getRating();
            ratingComment = incident.getFeedback().getComment();
        }

        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus().name(),
                priority,
                category,
                incident.getService().getName(),
                incident.getImpact().name(),
                incident.getUrgency().name(),
                incident.getReporter().getName(),
                technician,
                incident.getAffectedUsers(),
                incident.getCreatedAt(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt(),
                incident.getClosedAt(),
                incident.getResolutionNote(),
                incident.getClosureNote(),
                rating,
                ratingComment,
                incident.isAwaitingInfo());
    }
}