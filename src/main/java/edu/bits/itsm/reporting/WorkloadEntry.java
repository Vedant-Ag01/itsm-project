package edu.bits.itsm.reporting;

public record WorkloadEntry(
        String technicianId,
        long activeIncidentCount
) { }