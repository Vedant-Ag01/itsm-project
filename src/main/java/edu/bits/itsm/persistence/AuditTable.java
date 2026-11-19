package edu.bits.itsm.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditTable extends JpaRepository<AuditEntity, Long> {

    List<AuditEntity> findByIncidentIdOrderByIdAsc(String incidentId);

    List<AuditEntity> findAllByOrderByIdAsc();
}