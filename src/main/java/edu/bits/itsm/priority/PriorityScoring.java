package edu.bits.itsm.priority;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Urgency;

class PriorityScoring {

    static final int P1_MIN_SCORE = 18;
    static final int P2_MIN_SCORE = 9;
    static final int P3_MIN_SCORE = 4;

    private PriorityScoring() {
    }

    // Rejects a null rating or a negative affected-user count
    static void validate(Impact impact, Urgency urgency,
                         BusinessCriticality criticality, int affectedUsers) {
        if (impact == null) {
            throw new IllegalArgumentException("impact must not be null");
        }
        if (urgency == null) {
            throw new IllegalArgumentException("urgency must not be null");
        }
        if (criticality == null) {
            throw new IllegalArgumentException("criticality must not be null");
        }
        if (affectedUsers < 0) {
            throw new IllegalArgumentException(
                    "affectedUsers must not be negative, but was " + affectedUsers);
        }
    }

    //currenly considering score as impact*urgency*criticality
    static int baseScore(Impact impact, Urgency urgency, BusinessCriticality criticality) {
        return impact.getWeight() * urgency.getWeight() * criticality.getWeight();
    }


    static Priority toPriority(int score) {
        if (score >= P1_MIN_SCORE) {
            return Priority.P1;
        }
        else if (score >= P2_MIN_SCORE) {
            return Priority.P2;
        }
        else if (score >= P3_MIN_SCORE) {
            return Priority.P3;
        }
        else return Priority.P4;
    }
}
