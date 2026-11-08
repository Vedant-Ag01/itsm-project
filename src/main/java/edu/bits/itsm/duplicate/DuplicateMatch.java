package edu.bits.itsm.duplicate;

import edu.bits.itsm.domain.Incident;

public record DuplicateMatch(Incident other, double score) {
}