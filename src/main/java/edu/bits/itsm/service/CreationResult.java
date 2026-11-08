package edu.bits.itsm.service;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.duplicate.DuplicateMatch;

import java.util.List;

// What createIncident gives back: the new incident, and any incidents
// that look like the same problem.
public record CreationResult(Incident incident, List<DuplicateMatch> possibleDuplicates) {
}