package edu.bits.itsm.escalation;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.Priority;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class EscalationService {

    static final String DUPLICATE_REASON =
            "duplicate ID in this scan; only the first copy was considered";

   
    public record SkippedIncident(int index, String incidentId, String reason) {
    }

    private final EscalationPolicy policy;
    private final EscalationLevelStore store;
    private List<SkippedIncident> lastSkipped = List.of();

  
    public EscalationService(EscalationPolicy policy) {
        this(policy, new InMemoryEscalationLevelStore());
    }

    
    public EscalationService(EscalationPolicy policy, EscalationLevelStore store) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.policy = policy;
        this.store = store;
    }

    public synchronized List<EscalationAction> findEscalations(List<Incident> incidents,
                                                               LocalDateTime now) {
        if (incidents == null) {
            throw new IllegalArgumentException("incidents must not be null");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        List<EscalationAction> actions = new ArrayList<>();
        List<SkippedIncident> skips = new ArrayList<>();
        Map<String, EscalationLevel> toSave = new LinkedHashMap<>();
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
            if (!isWaitingForAcknowledgement(incident.getStatus())) {
                continue;
            }
            String problem = dataProblem(incident, now);
            if (problem != null) {
                skips.add(new SkippedIncident(i, id, problem));
                continue;
            }

            EscalationLevel current = currentLevel(id);
            Priority priority = incident.getPriority();
            Duration elapsed = Duration.between(incident.getCreatedAt(), now);
            EscalationLevel next = policy.nextLevel(priority, current, elapsed);
            checkPolicyResult(id, current, next);

            if (next != current) {
                actions.add(new EscalationAction(id, priority, current, next, elapsed, now,
                        reason(id, priority, current, next, elapsed)));
                toSave.put(id, next);
            }
        }

        
        if (!toSave.isEmpty()) {
            store.saveLevels(Collections.unmodifiableMap(toSave));
        }
        lastSkipped = List.copyOf(skips);
        return List.copyOf(actions);
    }



    public synchronized List<SkippedIncident> lastSkipped() {
        return lastSkipped;
    }

    private static boolean isWaitingForAcknowledgement(IncidentStatus status) {
        return status == IncidentStatus.CLASSIFIED || status == IncidentStatus.ASSIGNED;
    }

    private static String dataProblem(Incident incident, LocalDateTime now) {
        IncidentStatus status = incident.getStatus();
        if (incident.getAcknowledgedAt() != null) {
            return "status " + status + " but acknowledgedAt is set";
        }
        if (incident.getResolvedAt() != null) {
            return "status " + status + " but resolvedAt is set";
        }
        if (incident.getPriority() == null) {
            return "missing priority";
        }
        if (incident.getCreatedAt().isAfter(now)) {
            return "createdAt " + incident.getCreatedAt() + " is after the scan time " + now;
        }
        return null;
    }

    private EscalationLevel currentLevel(String id) {
        Optional<EscalationLevel> stored = store.findLevel(id);
        if (stored == null) {
            throw new IllegalStateException("level store returned null for incident " + id);
        }
        return stored.orElse(EscalationLevel.TECHNICIAN);
    }

    private static void checkPolicyResult(String id, EscalationLevel current, EscalationLevel next) {
        if (next == null) {
            throw new IllegalStateException("policy returned no level for incident " + id);
        }
        int step = next.ordinal() - current.ordinal();
        if (step != 0 && step != 1) {
            throw new IllegalStateException("policy moved incident " + id + " from " + current
                    + " to " + next + "; only staying or one step up is allowed");
        }
    }

    private static String reason(String id, Priority priority, EscalationLevel from,
                                 EscalationLevel to, Duration elapsed) {
        return priority + " incident " + id + " not acknowledged " + describe(elapsed)
                + " after creation: escalate from " + from + " to " + to + ".";
    }


    private static String describe(Duration d) {
        StringBuilder text = new StringBuilder();
        if (d.toHours() > 0) {
            text.append(d.toHours()).append(" h ");
        }
        text.append(d.toMinutesPart()).append(" min ").append(d.toSecondsPart()).append(" s");
        return text.toString();
    }
}
