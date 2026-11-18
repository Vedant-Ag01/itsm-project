package edu.bits.itsm.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceTable extends JpaRepository<ServiceEntity, Long> {

    List<ServiceEntity> findAllByOrderByNameAsc();

    List<ServiceEntity> findByActiveTrueOrderByNameAsc();

    Optional<ServiceEntity> findByNameIgnoreCase(String name);
}