package edu.bits.itsm.escalation;

import java.util.Map;
import java.util.Optional;


public interface EscalationLevelStore {


    Optional<EscalationLevel> findLevel(String incidentId);

    void saveLevels(Map<String, EscalationLevel> levels);
}
