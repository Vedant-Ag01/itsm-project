package edu.bits.itsm.service;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.escalation.EscalationAction;
import edu.bits.itsm.escalation.EscalationService;
import edu.bits.itsm.event.IncidentEvent;
import edu.bits.itsm.event.IncidentEventPublisher;
import edu.bits.itsm.event.IncidentEventType;
import edu.bits.itsm.sla.SlaAlert;
import edu.bits.itsm.sla.SlaMonitor;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

// Checks every incident for SLA trouble and for waiting too long, and turns
// each finding into an event so the audit log and notifications pick it up.
public class SlaEscalationJob {

    public record RunResult(int slaWarnings, int slaBreaches, int escalations) { }

    private final IncidentService incidentService;
    private final SlaMonitor slaMonitor;
    private final EscalationService escalationService;
    private final IncidentEventPublisher publisher;

    public SlaEscalationJob(IncidentService incidentService,
                            SlaMonitor slaMonitor,
                            EscalationService escalationService,
                            IncidentEventPublisher publisher) {
        this.incidentService = incidentService;
        this.slaMonitor = slaMonitor;
        this.escalationService = escalationService;
        this.publisher = publisher;
    }

    // Runs by itself every 30 seconds
    @Scheduled(initialDelay = 20000, fixedDelay = 30000)
    public void scheduledRun() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            System.err.println("SLA/escalation check failed: " + e.getMessage());
        }
    }

    // One full check. Also called by the admin "run now" endpoint.
    public synchronized RunResult runOnce() {
        LocalDateTime now = LocalDateTime.now();
        List<Incident> all = incidentService.getAllIncidents();
        int warnings = 0;
        int breaches = 0;
        int escalations = 0;

        for (SlaAlert alert : slaMonitor.check(all, now)) {
            IncidentEventType type;
            if (alert.type() == SlaAlert.Type.WARNING) {
                type = IncidentEventType.SLA_WARNING;
                warnings++;
            } else {
                type = IncidentEventType.SLA_BREACHED;
                breaches++;
            }
            publish(find(all, alert.incidentId()), type, alert.message(), now);
        }

        for (EscalationAction action : escalationService.findEscalations(all, now)) {
            escalations++;
            publish(find(all, action.incidentId()), IncidentEventType.ESCALATED,
                    action.reason(), now);
        }

        return new RunResult(warnings, breaches, escalations);
    }

    private Incident find(List<Incident> all, String id) {
        for (Incident incident : all) {
            if (incident.getId().equals(id)) {
                return incident;
            }
        }
        return null;
    }

    private void publish(Incident incident, IncidentEventType type, String message,
                         LocalDateTime now) {
        if (incident == null) {
            return;
        }
        publisher.publish(new IncidentEvent(incident.getId(), type, "System",
                incident.getStatus(), incident.getStatus(), message, now));
    }
}