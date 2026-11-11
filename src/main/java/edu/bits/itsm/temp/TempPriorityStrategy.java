package edu.bits.itsm.temp;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Urgency;
import edu.bits.itsm.priority.PriorityStrategy;

// Temporary. Replaced by B's real strategy later.
public class TempPriorityStrategy implements PriorityStrategy {

    @Override
    public Priority calculate(Impact impact, Urgency urgency,
                              BusinessCriticality criticality, int affectedUsers) {
        int score = impact.getWeight() + urgency.getWeight() + criticality.getWeight();
        if (score >= 8) {
            return Priority.P1;
        }
        if (score >= 6) {
            return Priority.P2;
        }
        if (score >= 4) {
            return Priority.P3;
        }
        return Priority.P4;
    }

    @Override
    public String getName() {
        return "Temporary score-based priority";
    }
}