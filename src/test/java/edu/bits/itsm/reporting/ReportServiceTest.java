package edu.bits.itsm.reporting;

import edu.bits.itsm.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportServiceTest {

    private final ReportService reportService = new ReportService();
    private final Employee emp = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private final ItService netService = new ItService("Campus WiFi", BusinessCriticality.MEDIUM);

    private Incident buildIncident(String id, String title, IncidentCategory cat, IncidentStatus status) {
        return Incident.restore(
                id, emp, title, "Description", netService, Impact.LOW, Urgency.LOW, 1,
                LocalDateTime.now(), status, cat, Priority.P3,
                null, null, null, null, null, null, List.of(), null
        );
    }

    @Test
    void testEmptyListReturnsZeroedReport() {
        IncidentOverviewReport report = reportService.overview(List.of(), inc -> "ON_TRACK");

        assertEquals(0, report.totalIncidents());
        assertEquals(0, report.openIncidents());
        assertEquals(0, report.slaViolations());
        assertTrue(reportService.categoryBreakdown(List.of()).isEmpty());
    }

    @Test
    void testOverviewAndCategoryBreakdown() {
        Incident inc1 = buildIncident("INC1", "VPN Down", IncidentCategory.NETWORK, IncidentStatus.NEW);
        Incident inc2 = buildIncident("INC2", "Laptop Crash", IncidentCategory.HARDWARE, IncidentStatus.CLOSED);
        Incident inc3 = buildIncident("INC3", "Wi-Fi Slow", IncidentCategory.NETWORK, IncidentStatus.ASSIGNED);

        List<Incident> sample = List.of(inc1, inc2, inc3);

        IncidentOverviewReport overview = reportService.overview(
                sample,
                inc -> inc.getId().equals("INC1") ? "BREACHED" : "ON_TRACK"
        );

        assertEquals(3, overview.totalIncidents());
        assertEquals(2, overview.openIncidents());
        assertEquals(1, overview.resolvedIncidents());
        assertEquals(1, overview.slaViolations());

        List<CategoryBreakdown> breakdown = reportService.categoryBreakdown(sample);
        assertEquals(2, breakdown.size());
        assertEquals("NETWORK", breakdown.get(0).category());
        assertEquals(2, breakdown.get(0).count());
    }
}