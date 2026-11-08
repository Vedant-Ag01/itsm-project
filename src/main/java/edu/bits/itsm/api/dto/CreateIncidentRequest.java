package edu.bits.itsm.api.dto;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.Urgency;

// Reporter fields are temporary. Once login exists (V8), the reporter
// will come from the logged-in user instead.
public record CreateIncidentRequest(
        String reporterName,
        String title,
        String description,
        String serviceName,
        BusinessCriticality criticality,
        Impact impact,
        Urgency urgency,
        int affectedUsers,
        IncidentCategory category) {
}