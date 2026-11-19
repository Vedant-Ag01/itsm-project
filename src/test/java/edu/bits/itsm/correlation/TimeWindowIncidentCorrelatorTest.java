package edu.bits.itsm.correlation;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Urgency;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeWindowIncidentCorrelatorTest {

    private final Employee reporter = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private final ItService wifiService = new ItService("Campus WiFi", BusinessCriticality.HIGH);
    private final ItService erpService = new ItService("ERP Portal", BusinessCriticality.HIGH);

    private Incident buildIncident(String id, ItService service, IncidentStatus status, LocalDateTime createdAt) {
        return Incident.restore(
                id, reporter, "Outage on " + service.getName(), "Connection failing",
                service, Impact.HIGH, Urgency.HIGH, 25, createdAt,
                status, IncidentCategory.NETWORK, Priority.P1,
                null, null, null, null,
                status == IncidentStatus.RESOLVED ? "Restarted AP" : null,
                null, List.of(), null
        );
    }

    @Test
    void groupsMultipleActiveIncidentsOnSameServiceWithinWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 19, 12, 0);
        TimeWindowIncidentCorrelator correlator = new TimeWindowIncidentCorrelator(Duration.ofMinutes(30), 2);

        Incident i1 = buildIncident("INC1", wifiService, IncidentStatus.NEW, now.minusMinutes(20));
        Incident i2 = buildIncident("INC2", wifiService, IncidentStatus.ASSIGNED, now.minusMinutes(10));
        Incident i3 = buildIncident("INC3", erpService, IncidentStatus.NEW, now.minusMinutes(5));

        List<CorrelationGroup> groups = correlator.findGroups(List.of(i1, i2, i3), now);

        assertEquals(1, groups.size());
        CorrelationGroup group = groups.get(0);
        assertEquals("Campus WiFi", group.serviceName());
        assertEquals(2, group.size());
        assertEquals(IncidentCategory.NETWORK, group.category());
    }

    @Test
    void ignoresIncidentsOutsideTimeWindowOrAlreadyResolved() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 19, 12, 0);
        TimeWindowIncidentCorrelator correlator = new TimeWindowIncidentCorrelator(Duration.ofMinutes(30), 2);

        Incident oldIncident = buildIncident("INC1", wifiService, IncidentStatus.NEW, now.minusMinutes(45));
        Incident resolvedIncident = buildIncident("INC2", wifiService, IncidentStatus.RESOLVED, now.minusMinutes(10));
        Incident activeIncident = buildIncident("INC3", wifiService, IncidentStatus.IN_PROGRESS, now.minusMinutes(5));

        List<CorrelationGroup> groups = correlator.findGroups(
                List.of(oldIncident, resolvedIncident, activeIncident), now);

        assertTrue(groups.isEmpty());
    }

    @Test
    void ordersGroupsByLargestClusterFirst() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 19, 12, 0);
        TimeWindowIncidentCorrelator correlator = new TimeWindowIncidentCorrelator();

        List<Incident> incidents = List.of(
                buildIncident("INC1", wifiService, IncidentStatus.NEW, now.minusMinutes(15)),
                buildIncident("INC2", wifiService, IncidentStatus.NEW, now.minusMinutes(10)),
                buildIncident("INC3", erpService, IncidentStatus.NEW, now.minusMinutes(12)),
                buildIncident("INC4", erpService, IncidentStatus.NEW, now.minusMinutes(8)),
                buildIncident("INC5", erpService, IncidentStatus.NEW, now.minusMinutes(2))
        );

        List<CorrelationGroup> groups = correlator.findGroups(incidents, now);

        assertEquals(2, groups.size());
        assertEquals("ERP Portal", groups.get(0).serviceName());
        assertEquals(3, groups.get(0).size());
        assertEquals("Campus WiFi", groups.get(1).serviceName());
        assertEquals(2, groups.get(1).size());
    }
}
