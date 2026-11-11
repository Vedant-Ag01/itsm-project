package edu.bits.itsm.api.dto;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.Urgency;

// The reporter is no longer sent. It is the logged-in user.
public record CreateIncidentRequest(
        String title,
        String description,
        String serviceName,
        BusinessCriticality criticality,
        Impact impact,
        Urgency urgency,
        int affectedUsers,
        IncidentCategory category) {
}