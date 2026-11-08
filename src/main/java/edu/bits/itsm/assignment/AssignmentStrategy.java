package edu.bits.itsm.assignment;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.Technician;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface AssignmentStrategy {
    // activeLoad = how many open incidents each technician has right now.
    // Return Optional.empty() when nobody qualifies.
    Optional<Technician> selectTechnician(Incident incident,
                                          List<Technician> candidates,
                                          Map<Technician, Integer> activeLoad);

    String getName();
}