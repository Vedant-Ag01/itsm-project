package edu.bits.itsm.config;

import edu.bits.itsm.persistence.JpaServiceRepository;
import edu.bits.itsm.persistence.ServiceTable;
import edu.bits.itsm.repository.ServiceRepository;
import edu.bits.itsm.service.ServiceCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfig {

    @Bean
    public ServiceRepository serviceRepository(ServiceTable table) {
        return new JpaServiceRepository(table);
    }

    @Bean
    public ServiceCatalog serviceCatalog(ServiceRepository repository) {
        return new ServiceCatalog(repository);
    }
}