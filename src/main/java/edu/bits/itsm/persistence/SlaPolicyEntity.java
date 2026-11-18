package edu.bits.itsm.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// One row per priority: "P1", "P2", "P3", "P4"
@Entity
@Table(name = "sla_policies")
public class SlaPolicyEntity {

    @Id
    public String priority;

    public long responseMinutes;

    public long resolutionMinutes;

    public SlaPolicyEntity() {
    }
}
