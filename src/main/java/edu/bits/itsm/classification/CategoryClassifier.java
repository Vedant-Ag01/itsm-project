package edu.bits.itsm.classification;

import java.util.Optional;

public interface CategoryClassifier {
    Optional<CategorySuggestion> suggest(String title, String description);
}
