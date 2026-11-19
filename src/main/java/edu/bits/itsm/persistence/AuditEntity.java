package edu.bits.itsm.persistence;

import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.event.IncidentEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// One row per audit record in the database
@Entity
@Table(name = "audit_records")
public class AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String incidentId;

    @Enumerated(EnumType.STRING)
    public IncidentEventType eventType;

    public String actor;

    @Enumerated(EnumType.STRING)
    public IncidentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    public IncidentStatus newStatus;

    @Column(length = 2000)
    public String description;

    @Column(name = "event_time")
    public LocalDateTime timestamp;

    public AuditEntity() {
    }
}