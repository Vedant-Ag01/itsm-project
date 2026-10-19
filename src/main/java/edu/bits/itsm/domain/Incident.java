package edu.bits.itsm.domain;

import java.time.Instant;

public class Incident {

    private final String id;
    private final Employee reporter;
    private final String description;
    private final Instant createdAt;

    private IncidentCategory category;
    private Priority priority;
    private IncidentStatus status;
    private Technician assignedTechnician;

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
        this.createdAt = Instant.now();
        this.status = IncidentStatus.NEW;
    }

    public String getId() {
        return id;
    }

    public Employee getReporter() {
        return reporter;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
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
        return status;
    }

    public Technician getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(Technician assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }
}