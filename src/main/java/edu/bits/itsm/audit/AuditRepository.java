package edu.bits.itsm.audit;

import java.util.List;

public interface AuditRepository {
    void save(AuditRecord record);
    List<AuditRecord> findByIncident(String incidentId);
    List<AuditRecord> findAll();
}