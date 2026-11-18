package edu.bits.itsm.api.dto;

import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.Technician;

import java.util.ArrayList;
import java.util.List;

public record TechnicianResponse(
        String id,
        String name,
        String email,
        boolean available,
        List<String> skills,
        int activeIncidents) {

    public static TechnicianResponse from(Technician technician, int activeIncidents) {
        List<String> skills = new ArrayList<>();
        for (IncidentCategory category : technician.getSkills()) {
            skills.add(category.name());
        }
        return new TechnicianResponse(technician.getId(), technician.getName(),
                technician.getEmail(), technician.isAvailable(), skills, activeIncidents);
    }
}