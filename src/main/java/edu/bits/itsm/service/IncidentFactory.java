package edu.bits.itsm.service;

import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Urgency;
import edu.bits.itsm.repository.IncidentRepository;

// Builds new incidents. It only creates them, saving is done by the service.
public class IncidentFactory {

    private final IncidentRepository repository;

    public IncidentFactory(IncidentRepository repository) {
        this.repository = repository;
    }

    public Incident create(Employee reporter, String title, String description,
                           ItService service, Impact impact, Urgency urgency,
                           int affectedUsers) {
        Incident.checkFieldSizes(title, description, affectedUsers);
        String id = repository.nextId();
        return new Incident(id, reporter, title, description,
                service, impact, urgency, affectedUsers);
    }
}