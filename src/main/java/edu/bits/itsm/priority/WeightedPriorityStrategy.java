package edu.bits.itsm.priority;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Urgency;


public class WeightedPriorityStrategy implements PriorityStrategy {

    static final int USERS_PER_BONUS_POINT = 20;

    @Override
    public Priority calculate(Impact impact, Urgency urgency,
                              BusinessCriticality criticality, int affectedUsers) {
        PriorityScoring.validate(impact, urgency, criticality, affectedUsers);
        int baseScore = PriorityScoring.baseScore(impact, urgency, criticality);
        // increase the base score by 1 for every 20 affected users
        int userBonus = affectedUsers / USERS_PER_BONUS_POINT;
        return PriorityScoring.toPriority(baseScore + userBonus);
    }

    @Override
    public String getName() {
        return "WeightedPriorityStrategy";
    }
}
