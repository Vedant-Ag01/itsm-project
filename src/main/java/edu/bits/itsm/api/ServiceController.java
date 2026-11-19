package edu.bits.itsm.api;

import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.ServiceInfo;
import edu.bits.itsm.service.ServiceCatalog;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ServiceController {

    public record CreateServiceRequest(String name, String description,
                                       BusinessCriticality criticality) { }

    public record UpdateServiceRequest(String description, BusinessCriticality criticality,
                                       Boolean active) { }

    private final ServiceCatalog catalog;

    public ServiceController(ServiceCatalog catalog) {
        this.catalog = catalog;
    }

    // For the "report an incident" dropdown: only services that are switched on
    @GetMapping("/api/services")
    public List<ServiceInfo> activeServices() {
        return catalog.activeServices();
    }

    @GetMapping("/api/admin/services")
    @AllowedRoles({Role.ADMIN})
    public List<ServiceInfo> allServices() {
        return catalog.allServices();
    }

    @PostMapping("/api/admin/services")
    @AllowedRoles({Role.ADMIN})
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceInfo create(@RequestBody CreateServiceRequest request) {
        return catalog.create(request.name(), request.description(), request.criticality());
    }

    @PutMapping("/api/admin/services/{id}")
    @AllowedRoles({Role.ADMIN})
    public ServiceInfo update(@PathVariable long id, @RequestBody UpdateServiceRequest request) {
        return catalog.update(id, request.description(), request.criticality(), request.active());
    }
}