package edu.bits.itsm.audit;

import edu.bits.itsm.event.IncidentEvent;
import edu.bits.itsm.event.IncidentObserver;

public class AuditLogObserver implements IncidentObserver {
    private final AuditRepository auditRepository;

    public AuditLogObserver(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void onEvent(IncidentEvent event) {
        AuditRecord record = new AuditRecord(
                event.incidentId(),
                event.type(),
                event.actor(),
                event.previousStatus() != null ? event.previousStatus().name() : null,
                event.newStatus() != null ? event.newStatus().name() : null,
                event.description(),
                event.timestamp()
        );
        auditRepository.save(record);
    }
}
