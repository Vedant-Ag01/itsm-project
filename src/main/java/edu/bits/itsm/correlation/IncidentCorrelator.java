package edu.bits.itsm.correlation;

import edu.bits.itsm.domain.Incident;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentCorrelator {
    List<CorrelationGroup> findGroups(List<Incident> incidents, LocalDateTime now);
}
