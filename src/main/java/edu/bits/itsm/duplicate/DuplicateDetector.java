package edu.bits.itsm.duplicate;

import edu.bits.itsm.domain.Incident;

import java.util.List;

public interface DuplicateDetector {
    List<DuplicateMatch> findSimilar(Incident incoming, List<Incident> existing);
}