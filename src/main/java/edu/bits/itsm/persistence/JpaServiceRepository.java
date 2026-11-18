package edu.bits.itsm.persistence;

import edu.bits.itsm.domain.ServiceInfo;
import edu.bits.itsm.repository.ServiceRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JpaServiceRepository implements ServiceRepository {

    private final ServiceTable table;

    public JpaServiceRepository(ServiceTable table) {
        this.table = table;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceInfo> findAll() {
        return toInfos(table.findAllByOrderByNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceInfo> findActive() {
        return toInfos(table.findByActiveTrueOrderByNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceInfo> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return table.findByNameIgnoreCase(name.trim()).map(this::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceInfo> findById(long id) {
        return table.findById(id).map(this::toInfo);
    }

    @Override
    @Transactional
    public ServiceInfo save(ServiceInfo info) {
        ServiceEntity entity;
        if (info.id() == null) {
            entity = new ServiceEntity();
        } else {
            entity = table.findById(info.id()).orElseThrow(
                    () -> new IllegalArgumentException("Unknown service id " + info.id()));
        }
        entity.name = info.name();
        entity.description = info.description();
        entity.criticality = info.criticality();
        entity.active = info.active();
        return toInfo(table.save(entity));
    }

    private ServiceInfo toInfo(ServiceEntity e) {
        return new ServiceInfo(e.id, e.name, e.description, e.criticality, e.active);
    }

    private List<ServiceInfo> toInfos(List<ServiceEntity> entities) {
        List<ServiceInfo> result = new ArrayList<>();
        for (ServiceEntity e : entities) {
            result.add(toInfo(e));
        }
        return result;
    }
}