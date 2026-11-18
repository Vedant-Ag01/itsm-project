package edu.bits.itsm.sla;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Priority;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.Urgency;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Printed demo with fixed times (no Spring, no database, no real clock).
// P1 incident INC1024 created 10:00, never acknowledged:
// response warning at 10:04, response due 10:05, breached after 10:05.
class SlaMonitorDemoTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 11, 11, 10, 0);

    // Test-only use of Incident.restore to get a fixed creation time.
    private static Incident unacknowledgedP1() {
        return Incident.restore("INC1024",
                new Employee("E1", "Asha", "asha@bits.edu", "Finance"),
                "Payment down", "Payments are failing",
                new ItService("Payment application", BusinessCriticality.HIGH),
                Impact.HIGH, Urgency.HIGH, 50, CREATED, IncidentStatus.ASSIGNED,
                IncidentCategory.APPLICATION, Priority.P1,
                new Technician("T1", "Ravi", "ravi@bits.edu"),
                null, null, null, null, null, new ArrayList<>(), null);
    }

    @Test
    void warningThenBreach() {
        SlaCalculator calculator = new DefaultSlaCalculator(new DefaultSlaPolicy());
        SlaMonitor monitor = new SlaMonitor(calculator);   // one monitor, reused
        List<Incident> incidents = List.of(unacknowledgedP1());

        List<SlaAlert> at1003 = runCheck(monitor, calculator, incidents, LocalDateTime.of(2026, 11, 11, 10, 3));
        List<SlaAlert> at1004 = runCheck(monitor, calculator, incidents, LocalDateTime.of(2026, 11, 11, 10, 4));
        List<SlaAlert> at1005 = runCheck(monitor, calculator, incidents, LocalDateTime.of(2026, 11, 11, 10, 5));
        List<SlaAlert> at1006 = runCheck(monitor, calculator, incidents, LocalDateTime.of(2026, 11, 11, 10, 5, 1));

        assertEquals(List.of(), at1003);
        assertEquals(1, at1004.size());
        assertEquals(SlaAlert.Type.WARNING, at1004.get(0).type());
        assertEquals(List.of(), at1005);
        assertEquals(1, at1006.size());
        assertEquals(SlaAlert.Type.BREACH, at1006.get(0).type());
    }

    // Runs one check and prints the time, the calculator's status and the alerts.
    private static List<SlaAlert> runCheck(SlaMonitor monitor, SlaCalculator calculator,
                                           List<Incident> incidents, LocalDateTime now) {
        SlaStatus status = calculator.evaluate(CREATED, Priority.P1, null, null, now);
        List<SlaAlert> alerts = monitor.check(incidents, now);
        System.out.println(now.toLocalTime() + " | status " + status + " | alerts: "
                + (alerts.isEmpty() ? "none" : alerts.size()));
        for (SlaAlert alert : alerts) {
            System.out.println("    " + alert.type() + ": " + alert.message());
        }
        return alerts;
    }
}
