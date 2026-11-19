package edu.bits.itsm.classification;

import edu.bits.itsm.domain.IncidentCategory;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class KeywordCategoryClassifierTest {

    private final CategoryClassifier classifier =
            new KeywordCategoryClassifier(KeywordCategoryClassifier.defaultRules());

    @Test
    void testSuggestsNetworkForVpnAndWifiIssue() {
        Optional<CategorySuggestion> result =
                classifier.suggest("VPN disconnects", "Cannot reach campus WiFi or DNS");

        assertTrue(result.isPresent());
        assertEquals(IncidentCategory.NETWORK, result.get().category());
        assertTrue(result.get().confidence() >= 0.8);
        assertTrue(result.get().matchedKeywords().contains("vpn"));
    }

    @Test
    void testReturnsEmptyWhenNoKeywordsMatch() {
        Optional<CategorySuggestion> result =
                classifier.suggest("Cafeteria menu", "What is for lunch today?");

        assertTrue(result.isEmpty());
    }

    @Test
    void testHandlesNullAndBlankInputsSafely() {
        assertTrue(classifier.suggest(null, "   ").isEmpty());
    }