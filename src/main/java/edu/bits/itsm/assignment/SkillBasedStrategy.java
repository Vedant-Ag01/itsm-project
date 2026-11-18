package edu.bits.itsm.assignment;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.Technician;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SkillBasedStrategy implements AssignmentStrategy {

    @Override
    public Optional<Technician> selectTechnician(Incident incident,
                                                 List<Technician> candidates,
                                                 Map<Technician, Integer> activeLoad) {
        AssignmentInputs.validate(incident, candidates, activeLoad);
        IncidentCategory category = incident.getCategory();
        if (category == null) {
            throw new IllegalArgumentException(
                    "Incident category is required for skill-based assignment.");
        }

        Technician best = null;
        int bestLoad = 0;  
        for (Technician technician : candidates) {
            if (technician == null || !technician.isAvailable()
                    || !technician.hasSkill(category)) {
                continue;
            }
            int load = AssignmentInputs.loadOf(technician, activeLoad);
            if (best == null || load < bestLoad) {
                best = technician;
                bestLoad = load;
            }
        }
        return Optional.ofNullable(best);
    }

    @Override
    public String getName() {
        return "SkillBasedStrategy";
    }
}
