package edu.bits.itsm.search;

import edu.bits.itsm.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IncidentSearchServiceTest {

    private final IncidentSearchService searchService = new IncidentSearchService();
    private final Employee emp = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private final ItService netService = new ItService("Campus WiFi", BusinessCriticality.MEDIUM);

    private Incident buildIncident(String id, String title, String desc, IncidentCategory cat, IncidentStatus status) {
        return Incident.restore(
                id, emp, title, desc, netService, Impact.LOW, Urgency.LOW, 1,
                LocalDateTime.now(), status, cat, Priority.P3,
                null, null, null, null, null, null, List.of(), null
        );
    }

    @Test
    void testSearchBySingleFilterAndFreeTextQuery() {
        Incident inc1 = buildIncident("INC101", "Wi-Fi Issue", "Slow internet on floor 3", IncidentCategory.NETWORK, IncidentStatus.NEW);
        Incident inc2 = buildIncident("INC102", "Printer Failure", "Paper jam in lab", IncidentCategory.HARDWARE, IncidentStatus.NEW);

        IncidentSearchCriteria criteria = new IncidentSearchCriteria.Builder()
                .category("NETWORK")
                .query("slow internet")
                .build();

        List<Incident> results = searchService.search(List.of(inc1, inc2), criteria, null);

        assertEquals(1, results.size());
        assertEquals("INC101", results.get(0).getId());
    }

    @Test
    void testSearchWithMultipleFiltersAndSlaEvaluator() {
        Incident inc1 = buildIncident("INC101", "Wi-Fi Issue", "Slow internet", IncidentCategory.NETWORK, IncidentStatus.NEW);
        Incident inc2 = buildIncident("INC102", "Router Down", "No connection", IncidentCategory.NETWORK, IncidentStatus.NEW);

        IncidentSearchCriteria criteria = new IncidentSearchCriteria.Builder()
                .category("NETWORK")
                .slaStatus("BREACHED")
                .build();

        List<Incident> results = searchService.search(
                List.of(inc1, inc2),
                criteria,
                inc -> inc.getId().equals("INC102") ? "BREACHED" : "ON_TRACK"
        );

        assertEquals(1, results.size());
        assertEquals("INC102", results.get(0).getId());
    }

    @Test
    void testEmptyCriteriaReturnsAllIncidents() {
        Incident inc1 = buildIncident("INC101", "Issue 1", "Desc 1", IncidentCategory.SOFTWARE, IncidentStatus.NEW);
        Incident inc2 = buildIncident("INC102", "Issue 2", "Desc 2", IncidentCategory.HARDWARE, IncidentStatus.CLOSED);

        IncidentSearchCriteria emptyCriteria = new IncidentSearchCriteria.Builder().build();
        List<Incident> results = searchService.search(List.of(inc1, inc2), emptyCriteria, null);

        assertEquals(2, results.size());
    }
}