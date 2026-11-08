package edu.bits.itsm.priority;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Urgency;

public interface PriorityStrategy {
    Priority calculate(Impact impact, Urgency urgency,
                       BusinessCriticality criticality, int affectedUsers);

    String getName();
}