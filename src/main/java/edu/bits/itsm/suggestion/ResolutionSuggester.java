package edu.bits.itsm.suggestion;

import edu.bits.itsm.domain.Incident;

import java.util.List;

public interface ResolutionSuggester {
    List<ResolutionSuggestion> suggest(Incident incoming, List<Incident> history, int limit);
}
