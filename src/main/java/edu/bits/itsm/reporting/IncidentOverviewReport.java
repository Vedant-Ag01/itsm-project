package edu.bits.itsm.reporting;

public record IncidentOverviewReport(
        long totalIncidents,
        long openIncidents,
        long criticalP1Incidents,
        long resolvedIncidents,
        long slaViolations
) { }