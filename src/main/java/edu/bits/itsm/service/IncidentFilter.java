package edu.bits.itsm.service;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.Priority;

import java.time.LocalDate;

// A set of optional search conditions. A null value means "do not filter on this".
public record IncidentFilter(
        IncidentStatus status,
        Priority priority,
        IncidentCategory category,
        String technicianId,
        String text,
        LocalDate createdFrom,
        LocalDate createdTo) {

    // true if the incident passes every condition that was given
    public boolean matches(Incident incident) {
        if (status != null && incident.getStatus() != status) {
            return false;
        }
        if (priority != null && incident.getPriority() != priority) {
            return false;
        }
        if (category != null && incident.getCategory() != category) {
            return false;
        }
        if (technicianId != null && !technicianId.isBlank()) {
            if (incident.getAssignedTechnician() == null
                    || !incident.getAssignedTechnician().getId().equals(technicianId)) {
                return false;
            }
        }
        if (text != null && !text.isBlank()) {
            String wanted = text.toLowerCase();
            boolean found = incident.getTitle().toLowerCase().contains(wanted)
                    || incident.getDescription().toLowerCase().contains(wanted)
                    || incident.getId().toLowerCase().contains(wanted);
            if (!found) {
                return false;
            }
        }
        if (createdFrom != null && incident.getCreatedAt().toLocalDate().isBefore(createdFrom)) {
            return false;
        }
        if (createdTo != null && incident.getCreatedAt().toLocalDate().isAfter(createdTo)) {
            return false;
        }
        return true;
    }
}