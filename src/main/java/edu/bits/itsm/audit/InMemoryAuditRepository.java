package edu.bits.itsm.audit;

import java.util.ArrayList;
import java.util.List;

public class InMemoryAuditRepository implements AuditRepository {
    private final List<AuditRecord> records = new ArrayList<>();

    @Override
    public synchronized void save(AuditRecord record) {
        records.add(record);
    }

    @Override
    public synchronized List<AuditRecord> findByIncident(String incidentId) {
        return records.stream()
                .filter(r -> r.incidentId().equals(incidentId))
                .toList();
    }

    @Override
    public synchronized List<AuditRecord> findAll() {
        return new ArrayList<>(records);
    }
}