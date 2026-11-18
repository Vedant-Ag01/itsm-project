package edu.bits.itsm.service;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.event.IncidentEvent;
import edu.bits.itsm.notification.RecipientResolver;
import edu.bits.itsm.repository.IncidentRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Decides who should be told about an event. Recipients are email addresses.
public class IncidentRecipientResolver implements RecipientResolver {

    private final IncidentRepository repository;
    private final List<String> adminEmails;

    public IncidentRecipientResolver(IncidentRepository repository, List<String> adminEmails) {
        this.repository = repository;
        this.adminEmails = adminEmails;
    }

    @Override
    public List<String> recipientsFor(IncidentEvent event) {
        Set<String> recipients = new LinkedHashSet<>();

        switch (event.type()) {
            case CREATED, DUPLICATE_FLAGGED -> recipients.addAll(adminEmails);
            case SLA_BREACHED, ESCALATED -> recipients.addAll(adminEmails);
            default -> { }
        }

        // The incident may not be saved yet (CREATED is published before saving),
        // so only look up reporter and technician when it exists.
        Optional<Incident> found = repository.findById(event.incidentId());
        if (found.isPresent()) {
            Incident incident = found.get();
            String reporter = incident.getReporter().getEmail();
            String technician = incident.getAssignedTechnician() == null
                    ? null : incident.getAssignedTechnician().getEmail();

            switch (event.type()) {
                case ASSIGNED -> { add(recipients, reporter); add(recipients, technician); }
                case ACKNOWLEDGED, WORK_STARTED, RESOLVED -> add(recipients, reporter);
                case VERIFIED, CLOSED -> { add(recipients, reporter); add(recipients, technician); }
                case SLA_WARNING -> add(recipients, technician);
                case SLA_BREACHED, ESCALATED -> { add(recipients, reporter); add(recipients, technician); }
                default -> { }
            }
        }
        return new ArrayList<>(recipients);
    }

    private void add(Set<String> recipients, String email) {
        if (email != null && !email.isBlank()) {
            recipients.add(email);
        }
    }
}