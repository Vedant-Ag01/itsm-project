package edu.bits.itsm.api;

import edu.bits.itsm.classification.CategoryClassifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ClassificationController {

    public record SuggestRequest(String title, String description) { }

    // category is null when nothing in the text pointed to a category
    public record SuggestResponse(String category, double confidence, List<String> matchedKeywords) { }

    private final CategoryClassifier classifier;

    public ClassificationController(CategoryClassifier classifier) {
        this.classifier = classifier;
    }

    // For the "report an incident" form: suggest a category while the user types
    @PostMapping("/api/classification/suggest")
    public SuggestResponse suggest(@RequestBody SuggestRequest request) {
        return classifier.suggest(request.title(), request.description())
                .map(s -> new SuggestResponse(s.category().name(), s.confidence(), s.matchedKeywords()))
                .orElse(new SuggestResponse(null, 0.0, List.of()));
    }
}
