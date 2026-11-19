package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Incident;

import java.time.LocalDateTime;

public interface SlaRiskPredictor {
    SlaRisk assess(Incident incident, int technicianActiveLoad, LocalDateTime now);
}
