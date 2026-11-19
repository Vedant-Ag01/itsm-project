package edu.bits.itsm.service;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.Urgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceAnalysisReportTest {

    private Employee asha;
    private Technician ravi;
    private ItService wifi;
    private ItService printing;

    @BeforeEach
    void setUp() {
        asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        ravi = new Technician("T1", "Ravi", "ravi@bits.edu");
        wifi = new ItService("Campus WiFi", BusinessCriticality.HIGH);
        printing = new ItService("Lab Printing", BusinessCriticality.LOW);
    }

    private Incident incident(String id, ItService service, Priority priority,
                              IncidentCategory category) {
        Incident incident = new Incident(id, asha, "Problem " + id, "Something broke", service,
                Impact.MEDIUM, Urgency.MEDIUM, 3);
        incident.setCategory(category);
        incident.setPriority(priority);
        incident.classify();
        return incident;
    }

    private void resolve(Incident incident) {
        incident.assign(ravi);
        incident.acknowledge();
        incident.startWork();
        incident.resolve("Fixed");
    }

    @Test
    void groupsIncidentsByServiceAndCountsThem() {
        List<Incident> incidents = new ArrayList<>();
        incidents.add(incident("INC1", wifi, Priority.P1, IncidentCategory.NETWORK));
        incidents.add(incident("INC2", wifi, Priority.P3, IncidentCategory.NETWORK));
        incidents.add(incident("INC3", wifi, Priority.P2, IncidentCategory.HARDWARE));
        incidents.add(incident("INC4", printing, Priority.P4, IncidentCategory.HARDWARE));
        resolve(incidents.get(1));

        List<ServiceAnalysisReport.Entry> report =
                ServiceAnalysisReport.build(incidents, i -> "ON_TRACK");

        assertEquals(2, report.size());
        ServiceAnalysisReport.Entry first = report.get(0);
        assertEquals("Campus WiFi", first.serviceName());
        assertEquals("HIGH", first.criticality());
        assertEquals(3, first.totalIncidents());
        assertEquals(2, first.openIncidents());
        assertEquals(1, first.resolvedIncidents());
        assertEquals(1, first.p1Incidents());
        assertEquals("NETWORK", first.mostCommonCategory());
        assertNotNull(first.averageResolutionMinutes());
    }

    @Test
    void mostTroubledServiceComesFirst() {
        List<Incident> incidents = new ArrayList<>();
        incidents.add(incident("INC1", printing, Priority.P4, IncidentCategory.HARDWARE));
        incidents.add(incident("INC2", wifi, Priority.P2, IncidentCategory.NETWORK));
        incidents.add(incident("INC3", wifi, Priority.P2, IncidentCategory.NETWORK));

        List<ServiceAnalysisReport.Entry> report =
                ServiceAnalysisReport.build(incidents, i -> "ON_TRACK");

        assertEquals("Campus WiFi", report.get(0).serviceName());
        assertEquals("Lab Printing", report.get(1).serviceName());
    }

    @Test
    void countsSlaBreachesAndHasNoAverageWhenNothingIsResolved() {
        List<Incident> incidents = new ArrayList<>();
        incidents.add(incident("INC1", wifi, Priority.P1, IncidentCategory.NETWORK));
        incidents.add(incident("INC2", wifi, Priority.P1, IncidentCategory.NETWORK));

        List<ServiceAnalysisReport.Entry> report = ServiceAnalysisReport.build(incidents,
                i -> i.getId().equals("INC2") ? "BREACHED" : "ON_TRACK");

        assertEquals(1, report.get(0).slaBreaches());
        assertNull(report.get(0).averageResolutionMinutes());
    }

    @Test
    void noIncidentsGivesAnEmptyReport() {
        assertTrue(ServiceAnalysisReport.build(new ArrayList<>(), i -> null).isEmpty());
    }
}
