package edu.bits.itsm.repository;

import edu.bits.itsm.domain.Incident;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Keeps incidents in a Map while the app runs. Everything is lost on restart,
// which is fine for now. The database version comes later.
public class InMemoryIncidentRepository implements IncidentRepository {

    private final Map<String, Incident> incidents = new LinkedHashMap<>();
    private int lastNumber = 1000;

    @Override
    public void save(Incident incident) {
        incidents.put(incident.getId(), incident);
    }

    @Override
    public Optional<Incident> findById(String id) {
        return Optional.ofNullable(incidents.get(id));
    }

    @Override
    public List<Incident> findAll() {
        return new ArrayList<>(incidents.values());
    }

    @Override
    public String nextId() {
        lastNumber++;
        return "INC" + lastNumber;
    }
}