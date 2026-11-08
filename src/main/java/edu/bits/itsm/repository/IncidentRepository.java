package edu.bits.itsm.repository;

import edu.bits.itsm.domain.Incident;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository {

    void save(Incident incident);

    Optional<Incident> findById(String id);

    List<Incident> findAll();

    // gives out the next free id like INC1001, INC1002...
    String nextId();
}