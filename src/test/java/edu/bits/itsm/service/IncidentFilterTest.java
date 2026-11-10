package edu.bits.itsm.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class IncidentFilterTest {

    private Incident vpnIncident;
    private Technician ravi;

    @BeforeEach
    void setUp() {
        // If this line is red, match it to your Employee constructor (same as the other tests)
        Employee asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        ItService vpn = new ItService("VPN", BusinessCriticality.MEDIUM);
        ravi = new Technician("T1", "Ravi", "ravi@bits.edu");

        vpnIncident = new Incident("INC1001", asha, "VPN down", "Cannot connect from home",
                vpn, Impact.MEDIUM, Urgency.HIGH, 10);
        vpnIncident.classify();
        vpnIncident.setCategory(IncidentCategory.NETWORK);
        vpnIncident.setPriority(Priority.P2);
        vpnIncident.assign(ravi);
    }

    private IncidentFilter onlyStatus(IncidentStatus status) {
        return new IncidentFilter(status, null, null, null, null, null, null);
    }

    @Test
    void anEmptyFilterMatchesEverything() {
        IncidentFilter empty = new IncidentFilter(null, null, null, null, null, null, null);

        assertTrue(empty.matches(vpnIncident));
    }

    @Test
    void statusFilterMatchesOnlyThatStatus() {
        assertTrue(onlyStatus(IncidentStatus.ASSIGNED).matches(vpnIncident));
        assertFalse(onlyStatus(IncidentStatus.CLOSED).matches(vpnIncident));
    }

    @Test
    void priorityAndCategoryFiltersWork() {
        IncidentFilter right = new IncidentFilter(null, Priority.P2,
                IncidentCategory.NETWORK, null, null, null, null);
        IncidentFilter wrong = new IncidentFilter(null, Priority.P1,
                IncidentCategory.NETWORK, null, null, null, null);

        assertTrue(right.matches(vpnIncident));
        assertFalse(wrong.matches(vpnIncident));
    }

    @Test
    void technicianFilterMatchesTheAssignedTechnician() {
        IncidentFilter forRavi = new IncidentFilter(null, null, null, "T1", null, null, null);
        IncidentFilter forMeera = new IncidentFilter(null, null, null, "T2", null, null, null);

        assertTrue(forRavi.matches(vpnIncident));
        assertFalse(forMeera.matches(vpnIncident));
    }

    @Test
    void textSearchIgnoresCaseAndLooksInTitleDescriptionAndId() {
        IncidentFilter inTitle = new IncidentFilter(null, null, null, null, "vpn", null, null);
        IncidentFilter inDescription = new IncidentFilter(null, null, null, null, "FROM HOME", null, null);
        IncidentFilter inId = new IncidentFilter(null, null, null, null, "inc1001", null, null);
        IncidentFilter nowhere = new IncidentFilter(null, null, null, null, "printer", null, null);

        assertTrue(inTitle.matches(vpnIncident));
        assertTrue(inDescription.matches(vpnIncident));
        assertTrue(inId.matches(vpnIncident));
        assertFalse(nowhere.matches(vpnIncident));
    }

    @Test
    void dateRangeIncludesTodayAndExcludesOtherDays() {
        LocalDate today = LocalDate.now();

        IncidentFilter includesToday = new IncidentFilter(null, null, null, null, null,
                today.minusDays(1), today.plusDays(1));
        IncidentFilter startsTomorrow = new IncidentFilter(null, null, null, null, null,
                today.plusDays(1), null);
        IncidentFilter endedYesterday = new IncidentFilter(null, null, null, null, null,
                null, today.minusDays(1));

        assertTrue(includesToday.matches(vpnIncident));
        assertFalse(startsTomorrow.matches(vpnIncident));
        assertFalse(endedYesterday.matches(vpnIncident));
    }

    @Test
    void allGivenConditionsMustMatchTogether() {
        IncidentFilter oneWrong = new IncidentFilter(IncidentStatus.ASSIGNED, Priority.P1,
                null, null, null, null, null);

        assertFalse(oneWrong.matches(vpnIncident));
    }
}