package edu.bits.itsm.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentTable extends JpaRepository<IncidentEntity, String> {
}