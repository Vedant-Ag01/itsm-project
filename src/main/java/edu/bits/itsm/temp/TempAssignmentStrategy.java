package edu.bits.itsm.temp;

import edu.bits.itsm.assignment.AssignmentStrategy;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.Technician;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Temporary. Picks the technician with the fewest active incidents.
public class TempAssignmentStrategy implements AssignmentStrategy {

    @Override
    public Optional<Technician> selectTechnician(Incident incident,
                                                 List<Technician> candidates,
                                                 Map<Technician, Integer> activeLoad) {
        Technician best = null;
        int bestLoad = Integer.MAX_VALUE;
        for (Technician t : candidates) {
            int load = activeLoad.getOrDefault(t, 0);
            if (load < bestLoad) {
                best = t;
                bestLoad = load;
            }
        }
        return Optional.ofNullable(best);
    }

    @Override
    public String getName() {
        return "Temporary least-loaded";
    }
}