package edu.bits.itsm.repository;

import edu.bits.itsm.domain.ServiceInfo;

import java.util.List;
import java.util.Optional;

// What the rest of the application may ask of the service catalogue storage
public interface ServiceRepository {

    List<ServiceInfo> findAll();

    List<ServiceInfo> findActive();

    Optional<ServiceInfo> findByName(String name);

    Optional<ServiceInfo> findById(long id);

    // creates the service if its id is null, otherwise updates that service
    ServiceInfo save(ServiceInfo service);
}