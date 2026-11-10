package edu.bits.itsm.persistence;

import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.repository.IncidentRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

// Saves incidents in PostgreSQL. Same job as InMemoryIncidentRepository.
@Transactional
public class JpaIncidentRepository implements IncidentRepository {

    private final IncidentTable jpa;
    private final IncidentMapper mapper;
    private final AtomicLong lastNumber;

    public JpaIncidentRepository(IncidentTable jpa, IncidentMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;

        // Continue numbering after the highest id already in the database
        long highest = 1000;
        for (IncidentEntity entity : jpa.findAll()) {
            try {
                long number = Long.parseLong(entity.id.substring(3));
                if (number > highest) {
                    highest = number;
                }
            } catch (NumberFormatException e) {
                // an id that does not look like INC1234, ignore it
            }
        }
        this.lastNumber = new AtomicLong(highest);
    }

    @Override
    public void save(Incident incident) {
        jpa.save(mapper.toEntity(incident));
    }

    @Override
    public Optional<Incident> findById(String id) {
        return jpa.findById(id).map(mapper::toIncident);
    }

    @Override
    public List<Incident> findAll() {
        List<Incident> incidents = new ArrayList<>();
        for (IncidentEntity entity : jpa.findAll()) {
            incidents.add(mapper.toIncident(entity));
        }
        return incidents;
    }

    @Override
    public String nextId() {
        return "INC" + lastNumber.incrementAndGet();
    }
}