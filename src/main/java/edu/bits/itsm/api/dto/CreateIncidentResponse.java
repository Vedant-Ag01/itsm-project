package edu.bits.itsm.api.dto;

import java.util.List;

public record CreateIncidentResponse(
        IncidentResponse incident,
        List<String> possibleDuplicateIds) {
}