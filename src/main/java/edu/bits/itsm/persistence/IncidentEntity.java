package edu.bits.itsm.persistence;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Urgency;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// One row in the "incidents" table. This is only a data holder for the database.
// The real behaviour (states, rules) stays in the Incident class.
@Entity
@Table(name = "incidents")
public class IncidentEntity {

    @Id
    public String id;

    // who reported it
    public String reporterId;
    public String reporterName;
    public String reporterEmail;
    public String reporterDepartment;

    public String title;

    @Column(length = 2000)
    public String description;

    // which IT service it is about
    public String serviceName;

    @Enumerated(EnumType.STRING)
    public BusinessCriticality serviceCriticality;

    @Enumerated(EnumType.STRING)
    public Impact impact;

    @Enumerated(EnumType.STRING)
    public Urgency urgency;

    public int affectedUsers;
    public LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    public IncidentCategory category;

    @Enumerated(EnumType.STRING)
    public Priority priority;

    // we only store the id and look the technician up again when loading
    public String assignedTechnicianId;

    // the state object cannot be stored, so we store just its status name
    @Enumerated(EnumType.STRING)
    public IncidentStatus status;

    public LocalDateTime acknowledgedAt;
    public LocalDateTime resolvedAt;
    public LocalDateTime closedAt;

    @Column(length = 2000)
    public String resolutionNote;

    @Column(length = 2000)
    public String closureNote;

    // feedback, if the employee gave any
    public Integer feedbackRating;

    @Column(length = 2000)
    public String feedbackComment;

    @ElementCollection
    @CollectionTable(name = "incident_notes", joinColumns = @JoinColumn(name = "incident_id"))
    public List<NoteData> notes = new ArrayList<>();
}