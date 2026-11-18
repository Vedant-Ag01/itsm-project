package edu.bits.itsm.assignment;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.Technician;

import java.util.List;
import java.util.Map;

class AssignmentInputs {

    private AssignmentInputs() {
    }

    static void validate(Incident incident,
                         List<Technician> candidates,
                         Map<Technician, Integer> activeLoad) {
        if (incident == null) {
            throw new IllegalArgumentException("Incident must not be null.");
        }
        if (candidates == null) {
            throw new IllegalArgumentException("Candidate list must not be null.");
        }
        if (activeLoad == null) {
            return;   // consider everyone has 0 active incidents
        }
        for (Map.Entry<Technician, Integer> entry : activeLoad.entrySet()) {
            Technician technician = entry.getKey();
            if (technician == null) {
                continue; 
            }
            Integer load = entry.getValue();
            if (load != null && load < 0) {
                throw new IllegalArgumentException("Workload for technician "
                        + technician.getId() + " cannot be negative: " + load+ ".");
            }
        }
    }

    static int loadOf(Technician technician, Map<Technician, Integer> activeLoad) {
        if (activeLoad == null) {
            return 0;
        }
        Integer load = activeLoad.get(technician);
        return load == null ? 0 : load;
    }
}
