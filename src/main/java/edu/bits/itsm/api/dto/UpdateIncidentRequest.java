package edu.bits.itsm.api.dto;

import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Urgency;

// Every field is optional: leave one out (or null) to keep its current value
public record UpdateIncidentRequest(
        String title,
        String description,
        Impact impact,
        Urgency urgency,
        Integer affectedUsers) {
}
