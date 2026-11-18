package edu.bits.itsm.escalation;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryEscalationLevelStoreTest {

    private final InMemoryEscalationLevelStore store = new InMemoryEscalationLevelStore();

    @Test
    void unknownIncidentHasNoStoredLevel() {
        assertEquals(Optional.empty(), store.findLevel("INC1"));
    }

    @Test
    void savedLevelsAreFoundAndCanBeRaised() {
        store.saveLevels(Map.of("A", EscalationLevel.TEAM_LEAD, "B", EscalationLevel.SYSTEM_ADMIN));
        store.saveLevels(Map.of("A", EscalationLevel.SYSTEM_ADMIN));

        assertEquals(Optional.of(EscalationLevel.SYSTEM_ADMIN), store.findLevel("A"));
        assertEquals(Optional.of(EscalationLevel.SYSTEM_ADMIN), store.findLevel("B"));
    }

    @Test
    void invalidBatchSavesNothing() {
        Map<String, EscalationLevel> batch = new HashMap<>();
        batch.put("A", EscalationLevel.TEAM_LEAD);
        batch.put("B", null);

        assertThrows(IllegalArgumentException.class, () -> store.saveLevels(batch));
        assertEquals(Optional.empty(), store.findLevel("A"));
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> store.findLevel(null));
        assertThrows(IllegalArgumentException.class, () -> store.saveLevels(null));
        Map<String, EscalationLevel> nullKey = new HashMap<>();
        nullKey.put(null, EscalationLevel.TEAM_LEAD);
        assertThrows(IllegalArgumentException.class, () -> store.saveLevels(nullKey));
    }
}
