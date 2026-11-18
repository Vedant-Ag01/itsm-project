package edu.bits.itsm.escalation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class InMemoryEscalationLevelStore implements EscalationLevelStore {

    private final Map<String, EscalationLevel> levels = new HashMap<>();

    @Override
    public synchronized Optional<EscalationLevel> findLevel(String incidentId) {
        if (incidentId == null) {
            throw new IllegalArgumentException("incidentId must not be null");
        }
        return Optional.ofNullable(levels.get(incidentId));
    }

   
    @Override
    public synchronized void saveLevels(Map<String, EscalationLevel> newLevels) {
        if (newLevels == null) {
            throw new IllegalArgumentException("levels must not be null");
        }
        for (Map.Entry<String, EscalationLevel> entry : newLevels.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("incident IDs and levels must not be null");
            }
        }
        levels.putAll(newLevels);
    }
}
