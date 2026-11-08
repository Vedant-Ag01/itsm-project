package edu.bits.itsm.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentJpaRepository extends JpaRepository<IncidentEntity, String> {
}