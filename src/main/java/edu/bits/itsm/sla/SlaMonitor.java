package edu.bits.itsm.sla;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SlaMonitor {

    static final String DUPLICATE_REASON =
            "duplicate ID in this check; only the first copy was considered";

    public record SkippedIncident(int index, String incidentId, String reason) {
    }

    private final SlaCalculator calculator;
   
    private Map<String, SlaStatus> memory = new HashMap<>();
    private List<SkippedIncident> lastSkipped = List.of();


    public SlaMonitor(SlaCalculator calculator) {
        if (calculator == null) {
            throw new IllegalArgumentException("calculator must not be null");
        }
        this.calculator = calculator;
    }


    public synchronized List<SlaAlert> check(List<Incident> incidents, LocalDateTime now) {
        if (incidents == null) {
            throw new IllegalArgumentException("incidents must not be null");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        Map<String, SlaStatus> working = new HashMap<>(memory);
        List<SlaAlert> alerts = new ArrayList<>();
        List<SkippedIncident> skips = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (int i = 0; i < incidents.size(); i++) {
            Incident incident = incidents.get(i);
            if (incident == null) {
                skips.add(new SkippedIncident(i, null, "null entry"));
                continue;
            }
            String id = incident.getId();
   
            if (!seenIds.add(id)) {
                skips.add(new SkippedIncident(i, id, DUPLICATE_REASON));
                continue;
            }
            boolean completed = isCompleted(incident.getStatus());
            String mismatch = statusMismatch(incident, completed);
            if (mismatch != null) {
                skips.add(new SkippedIncident(i, id, mismatch));
                continue;
            }
  
            if (completed && !working.containsKey(id)) {
                continue;
            }
            if (incident.getPriority() == null) {
                skips.add(new SkippedIncident(i, id, "missing priority"));
                continue;
            }
            SlaStatus status;
            try {
                status = calculator.evaluate(incident.getCreatedAt(), incident.getPriority(),
                        incident.getAcknowledgedAt(), incident.getResolvedAt(), now);
            } catch (IllegalArgumentException e) {
                // Bad incident data: skip it and keep its memory as it was.
                skips.add(new SkippedIncident(i, id, "invalid SLA data: " + e.getMessage()));
                continue;
            }
            if (status == null) {
                throw new IllegalStateException("calculator returned no status for incident " + id);
            }
            SlaStatus previous = working.get(id);

            if (completed) {
                // Final check: report a breach not alerted before, then forget the incident.
                if (status == SlaStatus.BREACHED && previous != SlaStatus.BREACHED) {
                    alerts.add(breach(incident, now));
                }
                working.remove(id);
            } else {
                if (status != previous && status == SlaStatus.AT_RISK) {
                    alerts.add(warning(incident, now));
                }
                if (status != previous && status == SlaStatus.BREACHED) {
                    alerts.add(breach(incident, now));
                }
  
                working.put(id, status);
            }
        }

  
        memory = working;
        lastSkipped = List.copyOf(skips);
        return List.copyOf(alerts);
    }


    public synchronized List<SkippedIncident> lastSkipped() {
        return lastSkipped;
    }

    private static boolean isCompleted(IncidentStatus status) {
        return status == IncidentStatus.RESOLVED
                || status == IncidentStatus.VERIFIED
                || status == IncidentStatus.CLOSED;
    }

    private static String statusMismatch(Incident incident, boolean completed) {
        if (completed && incident.getResolvedAt() == null) {
            return "status " + incident.getStatus() + " but resolvedAt is missing";
        }
        if (!completed && incident.getResolvedAt() != null) {
            return "status " + incident.getStatus() + " but resolvedAt is set";
        }
        return null;
    }

    private static SlaAlert warning(Incident incident, LocalDateTime now) {
        return new SlaAlert(incident.getId(), SlaAlert.Type.WARNING,
                "SLA warning for incident " + incident.getId() + " (" + incident.getPriority()
                        + ") found at " + now
                        + ": it has reached the 80% warning point of its response or resolution time.");
    }

    private static SlaAlert breach(Incident incident, LocalDateTime now) {
        return new SlaAlert(incident.getId(), SlaAlert.Type.BREACH,
                "SLA breach for incident " + incident.getId() + " (" + incident.getPriority()
                        + ") found at " + now + ": a response or resolution deadline was missed.");
    }
}
