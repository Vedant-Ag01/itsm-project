package edu.bits.itsm.config;

import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.duplicate.DuplicateDetector;
import edu.bits.itsm.duplicate.DuplicateMatch;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.event.IncidentObserver;
import edu.bits.itsm.repository.InMemoryIncidentRepository;
import edu.bits.itsm.repository.IncidentRepository;
import edu.bits.itsm.service.IncidentFactory;
import edu.bits.itsm.service.IncidentService;
import edu.bits.itsm.temp.TempAssignmentStrategy;
import edu.bits.itsm.temp.TempPriorityStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public IncidentRepository incidentRepository() {
        return new InMemoryIncidentRepository();
    }

    @Bean
    public List<Technician> technicians() {
        List<Technician> list = new ArrayList<>();
        list.add(new Technician("T1", "Ravi", "ravi@bits.edu"));
        list.add(new Technician("T2", "Meera", "meera@bits.edu"));
        return list;
    }

    @Bean
    public IncidentService incidentService(IncidentRepository repository,
                                           List<Technician> technicians) {
        // Temporary: no duplicate detection until C's detector is plugged in
        DuplicateDetector noDuplicates = new DuplicateDetector() {
            @Override
            public List<DuplicateMatch> findSimilar(Incident incoming, List<Incident> existing) {
                return new ArrayList<>();
            }
        };

        // Temporary: no observers until C's audit/notification code is plugged in
        List<IncidentObserver> observers = new ArrayList<>();

        return new IncidentService(
                repository,
                new IncidentFactory(repository),
                new TempPriorityStrategy(),
                new TempAssignmentStrategy(),
                noDuplicates,
                observers,
                technicians);
    }
}