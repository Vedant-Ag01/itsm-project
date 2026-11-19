package edu.bits.itsm.suggestion;

public record ResolutionSuggestion(
        String incidentId,
        String title,
        String resolutionNote,
        double similarity
) { }