package edu.bits.itsm.search;

import java.time.LocalDateTime;

public class IncidentSearchCriteria {
    private final String incidentId;
    private final LocalDateTime fromDate;
    private final LocalDateTime toDate;
    private final String status;
    private final String priority;
    private final String category;
    private final String service;
    private final String technicianId;
    private final String slaStatus;

    public IncidentSearchCriteria(Builder builder) {
        this.incidentId = builder.incidentId;
        this.fromDate = builder.fromDate;
        this.toDate = builder.toDate;
        this.status = builder.status;
        this.priority = builder.priority;
        this.category = builder.category;
        this.service = builder.service;
        this.technicianId = builder.technicianId;
        this.slaStatus = builder.slaStatus;
    }

    public String getIncidentId() { return incidentId; }
    public LocalDateTime getFromDate() { return fromDate; }
    public LocalDateTime getToDate() { return toDate; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public String getCategory() { return category; }
    public String getService() { return service; }
    public String getTechnicianId() { return technicianId; }
    public String getSlaStatus() { return slaStatus; }

    public static class Builder {
        private String incidentId;
        private LocalDateTime fromDate;
        private LocalDateTime toDate;
        private String status;
        private String priority;
        private String category;
        private String service;
        private String technicianId;
        private String slaStatus;

        public Builder incidentId(String incidentId) { this.incidentId = incidentId; return this; }
        public Builder fromDate(LocalDateTime fromDate) { this.fromDate = fromDate; return this; }
        public Builder toDate(LocalDateTime toDate) { this.toDate = toDate; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder priority(String priority) { this.priority = priority; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder service(String service) { this.service = service; return this; }
        public Builder technicianId(String technicianId) { this.technicianId = technicianId; return this; }
        public Builder slaStatus(String slaStatus) { this.slaStatus = slaStatus; return this; }

        public IncidentSearchCriteria build() {
            return new IncidentSearchCriteria(this);
        }
    }
}