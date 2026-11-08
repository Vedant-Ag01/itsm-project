package edu.bits.itsm.domain;

import java.time.LocalDateTime;

public class Incident {

    private final String id;
    private final Employee reporter;
    private final String description;
    private final LocalDateTime createdAt;

    private IncidentCategory category;
    private Priority priority;
    private Technician assignedTechnician;

    // The current state object Status is read from it, and it changes only
    // through the methods below.
    private IncidentState state = new NewState();

    // Timestamps the SLA code will need later.
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    public Incident(String id, Employee reporter, String description) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Incident id cannot be empty");
        }
        if (reporter == null) {
            throw new IllegalArgumentException("Incident must have a reporter");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Incident description cannot be empty");
        }
        this.id = id;
        this.reporter = reporter;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    // ---------- the only way status changes ----------

    public void classify() {
        state = state.classify();
    }

    public void assign(Technician technician) {
        if (technician == null) {
            throw new IllegalArgumentException("Cannot assign to a missing technician");
        }
        state = state.assign();              // throws first if illegal
        this.assignedTechnician = technician;
    }

    public void acknowledge() {
        state = state.acknowledge();
        this.acknowledgedAt = LocalDateTime.now();
    }

    public void startWork() {
        state = state.startWork();
    }

    public void resolve() {
        state = state.resolve();
        this.resolvedAt = LocalDateTime.now();
    }

    public void verify() {
        state = state.verify();
    }

    public void close() {
        state = state.close();
        this.closedAt = LocalDateTime.now();
    }

    // ---------- getters and the two simple setters ----------

    public String getId() {
        return id;
    }

    public Employee getReporter() {
        return reporter;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public IncidentStatus getStatus() {
        return state.getStatus();
    }

    public Technician getAssignedTechnician() {
        return assignedTechnician;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}