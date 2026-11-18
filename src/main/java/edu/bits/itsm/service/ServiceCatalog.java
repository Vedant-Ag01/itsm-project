package edu.bits.itsm.service;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.ServiceInfo;
import edu.bits.itsm.domain.ServiceNotFoundException;
import edu.bits.itsm.repository.ServiceRepository;

import java.util.List;

// The rules for the service catalogue. It only knows the ServiceRepository interface.
public class ServiceCatalog {

    private final ServiceRepository repository;

    public ServiceCatalog(ServiceRepository repository) {
        this.repository = repository;
    }

    public List<ServiceInfo> activeServices() {
        return repository.findActive();
    }

    public List<ServiceInfo> allServices() {
        return repository.findAll();
    }

    // Used when an incident is reported: the service must exist and be switched on
    public ServiceInfo requireActive(String name) {
        return repository.findByName(name)
                .filter(ServiceInfo::active)
                .orElseThrow(() -> new ServiceNotFoundException(name));
    }

    public ServiceInfo create(String name, String description, BusinessCriticality criticality) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Service name cannot be empty");
        }
        if (repository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("A service named '" + name.trim() + "' already exists");
        }
        return repository.save(new ServiceInfo(null, name, description, criticality, true));
    }

    // Anything left null keeps its current value
    public ServiceInfo update(long id, String description, BusinessCriticality criticality,
                              Boolean active) {
        ServiceInfo current = repository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("Unknown service id " + id));
        return repository.save(new ServiceInfo(
                current.id(),
                current.name(),
                description != null ? description : current.description(),
                criticality != null ? criticality : current.criticality(),
                active != null ? active : current.active()));
    }
}