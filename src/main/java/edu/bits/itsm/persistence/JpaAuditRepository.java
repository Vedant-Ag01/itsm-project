package edu.bits.itsm.persistence;

import edu.bits.itsm.audit.AuditRecord;
import edu.bits.itsm.audit.AuditRepository;

import java.util.ArrayList;
import java.util.List;

// Keeps audit records in PostgreSQL. Other code only knows the AuditRepository interface.
public class JpaAuditRepository implements AuditRepository {

    private final AuditTable table;

    public JpaAuditRepository(AuditTable table) {
        this.table = table;
    }

    @Override
    public void save(AuditRecord record) {
        AuditEntity entity = new AuditEntity();
        entity.incidentId = record.incidentId();
        entity.eventType = record.eventType();
        entity.actor = record.actor();
        entity.previousStatus = record.previousStatus();
        entity.newStatus = record.newStatus();
        entity.description = record.description();
        entity.timestamp = record.timestamp();
        table.save(entity);
    }

    @Override
    public List<AuditRecord> findByIncident(String incidentId) {
        return toRecords(table.findByIncidentIdOrderByIdAsc(incidentId));
    }

    @Override
    public List<AuditRecord> findAll() {
        return toRecords(table.findAllByOrderByIdAsc());
    }

    private List<AuditRecord> toRecords(List<AuditEntity> entities) {
        List<AuditRecord> records = new ArrayList<>();
        for (AuditEntity e : entities) {
            records.add(new AuditRecord(e.incidentId, e.eventType, e.actor,
                    e.previousStatus, e.newStatus, e.description, e.timestamp));
        }
        return records;
    }
}