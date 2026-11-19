package edu.bits.itsm.classification;

import edu.bits.itsm.domain.IncidentCategory;
import java.util.List;

public record CategorySuggestion(
        IncidentCategory category,
        double confidence,
        List<String> matchedKeywords
) { }
