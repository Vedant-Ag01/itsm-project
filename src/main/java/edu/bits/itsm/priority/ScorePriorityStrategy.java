package edu.bits.itsm.priority;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Urgency;


public class ScorePriorityStrategy implements PriorityStrategy {


    @Override
    public Priority calculate(Impact impact, Urgency urgency,
                              BusinessCriticality criticality, int affectedUsers) {
        PriorityScoring.validate(impact, urgency, criticality, affectedUsers);
        int score = PriorityScoring.baseScore(impact, urgency, criticality);
        return PriorityScoring.toPriority(score);
    }

    @Override
    public String getName() {
        return "ScorePriorityStrategy";
    }
}
