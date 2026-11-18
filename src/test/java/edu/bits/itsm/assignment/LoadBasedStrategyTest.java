package edu.bits.itsm.assignment;

import edu.bits.itsm.domain.BusinessCriticality;
import edu.bits.itsm.domain.Employee;
import edu.bits.itsm.domain.Impact;
import edu.bits.itsm.domain.Incident;
import edu.bits.itsm.domain.IncidentCategory;
import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.domain.ItService;
import edu.bits.itsm.domain.Technician;
import edu.bits.itsm.domain.Urgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoadBasedStrategyTest {

    private final LoadBasedStrategy strategy = new LoadBasedStrategy();

 
    private Technician a;
    private Technician b;
    private Technician c;
    private Technician d;
    private Incident incident;

    @BeforeEach
    void setUp() {
        a = technician("T1", "A", IncidentCategory.NETWORK);
        b = technician("T2", "B", IncidentCategory.SOFTWARE);
        c = technician("T3", "C", IncidentCategory.NETWORK);
        d = technician("T4", "D", IncidentCategory.NETWORK);
        d.setAvailable(false);
        incident = incident(IncidentCategory.NETWORK);
    }

    private static Technician technician(String id, String name, IncidentCategory skill) {
        Technician technician = new Technician(id, name, name.toLowerCase() + "@bits.edu");
        if (skill != null) {
            technician.addSkill(skill);
        }
        return technician;
    }

    private static Incident incident(IncidentCategory category) {
        Employee asha = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
        ItService vpn = new ItService("VPN", BusinessCriticality.HIGH);
        Incident incident = new Incident("INC1001", asha, "VPN down", "Cannot connect to VPN",
                vpn, Impact.HIGH, Urgency.HIGH, 10);
        incident.setCategory(category);
        return incident;
    }

    private Map<Technician, Integer> fixtureLoads() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(a, 5);
        load.put(b, 0);
        load.put(c, 2);
        load.put(d, 0);
        return load;
    }

    // ---------- normal selection ----------

    @Test
    void picksLowestLoadAmongAvailableTechniciansRegardlessOfSkill() {
        List<Technician> candidates = new ArrayList<>(List.of(a, b, c, d));

        Optional<Technician> chosen = strategy.selectTechnician(incident, candidates, fixtureLoads());

        // D (0) is unavailable; B (0) wins even without the NETWORK skill
        assertSame(b, chosen.orElseThrow());
    }

    @Test
    void unavailableTechnicianIsSkippedEvenWithTheBestLoad() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(d, 0);
        load.put(b, 3);

        Optional<Technician> chosen = strategy.selectTechnician(incident, List.of(d, b), load);

        assertSame(b, chosen.orElseThrow());
        assertFalse(d.isAvailable());   // skipped, not changed
    }

    @Test
    void tieGoesToFirstAvailableCandidateInListOrder() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(a, 2);
        load.put(c, 2);

        assertSame(a, strategy.selectTechnician(incident, List.of(a, c), load).orElseThrow());
        assertSame(c, strategy.selectTechnician(incident, List.of(c, a), load).orElseThrow());
    }

    @Test
    void techniciansWithNoSkillsCanWin() {
        Technician e = technician("T5", "E", null);
        Technician f = technician("T6", "F", null);
        Map<Technician, Integer> load = new HashMap<>();
        load.put(e, 4);
        load.put(f, 1);

        assertSame(f, strategy.selectTechnician(incident, List.of(e, f), load).orElseThrow());
    }

    @Test
    void missingCategoryDoesNotStopSelection() {
        Incident uncategorised = incident(null);

        Optional<Technician> chosen =
                strategy.selectTechnician(uncategorised, List.of(a, b, c, d), fixtureLoads());

        assertSame(b, chosen.orElseThrow());
    }

    @Test
    void soleAvailableCandidateWithMaxIntegerLoadIsSelected() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(a, Integer.MAX_VALUE);

        assertSame(a, strategy.selectTechnician(incident, List.of(a), load).orElseThrow());
    }

    // ---------- no match ----------

    @Test
    void nobodyAvailableGivesEmpty() {
        a.setAvailable(false);
        b.setAvailable(false);
        c.setAvailable(false);

        assertTrue(strategy.selectTechnician(incident, List.of(a, b, c, d), fixtureLoads())
                .isEmpty());
    }

    @Test
    void emptyCandidateListGivesEmpty() {
        assertTrue(strategy.selectTechnician(incident, List.of(), fixtureLoads()).isEmpty());
    }

    // ---------- null entries in the list ----------

    @Test
    void nullEntriesAreSkippedAndListIsUnchanged() {
        List<Technician> candidates = Arrays.asList(a, null, c);
        List<Technician> before = new ArrayList<>(candidates);

        Optional<Technician> chosen = strategy.selectTechnician(incident, candidates, fixtureLoads());

        assertSame(c, chosen.orElseThrow());   // A 5, C 2
        assertEquals(before, candidates);
    }

    @Test
    void listOfOnlyNullEntriesGivesEmpty() {
        assertTrue(strategy.selectTechnician(incident, Arrays.asList(null, null), fixtureLoads())
                .isEmpty());
    }

    // ---------- workload defaults ----------

    @Test
    void candidateMissingFromMapCountsAsZeroAndMapIsNotChanged() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(a, 3);

        Optional<Technician> chosen = strategy.selectTechnician(incident, List.of(a, c), load);

        assertSame(c, chosen.orElseThrow());
        assertEquals(1, load.size());
        assertFalse(load.containsKey(c));
    }

    @Test
    void emptyMapGivesFirstAvailableCandidate() {
        Optional<Technician> chosen =
                strategy.selectTechnician(incident, List.of(d, b, a), new HashMap<>());

        assertSame(b, chosen.orElseThrow());   // all 0; D is unavailable
    }

    @Test
    void nullMapTreatsEveryoneAsZeroButStillSkipsUnavailable() {
        Optional<Technician> chosen =
                strategy.selectTechnician(incident, List.of(d, a, b), null);

        assertSame(a, chosen.orElseThrow());
    }

    @Test
    void explicitNullLoadCountsAsZero() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(a, 1);
        load.put(c, null);

        assertSame(c, strategy.selectTechnician(incident, List.of(a, c), load).orElseThrow());
        assertNull(load.get(c));
        assertTrue(load.containsKey(c));
    }

    @Test
    void nullKeyInMapIsIgnored() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(null, 0);
        load.put(a, 2);
        load.put(c, 1);

        assertSame(c, strategy.selectTechnician(incident, List.of(a, c), load).orElseThrow());
    }

    @Test
    void nullKeyWithNegativeValueIsIgnored() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(null, -5);
        load.put(a, 1);

        assertSame(a, strategy.selectTechnician(incident, List.of(a), load).orElseThrow());
    }

    @Test
    void unmodifiableInputsWork() {
        Map<Technician, Integer> load = Map.of(a, 5, b, 0, c, 2, d, 0);

        assertSame(b, strategy.selectTechnician(incident, List.of(a, b, c, d), load).orElseThrow());
    }

    // ---------- invalid input ----------

    @Test
    void nullIncidentIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> strategy.selectTechnician(null, List.of(a), fixtureLoads()));
        assertEquals("Incident must not be null.", error.getMessage());
    }

    @Test
    void nullCandidateListIsRejected() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> strategy.selectTechnician(incident, null, fixtureLoads()));
        assertEquals("Candidate list must not be null.", error.getMessage());
    }

    @Test
    void negativeCandidateLoadIsRejected() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(b, -3);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> strategy.selectTechnician(incident, List.of(a, b), load));
        assertEquals("Workload for technician T2 cannot be negative: -3.", error.getMessage());
    }

    @Test
    void negativeLoadForNonCandidateIsRejected() {
        Technician outsider = technician("T9", "Outsider", null);
        Map<Technician, Integer> load = new HashMap<>();
        load.put(a, 1);
        load.put(outsider, -2);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> strategy.selectTechnician(incident, List.of(a), load));
        assertEquals("Workload for technician T9 cannot be negative: -2.", error.getMessage());
    }

    @Test
    void negativeLoadIsRejectedEvenWhenCandidateListIsEmpty() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(a, -1);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> strategy.selectTechnician(incident, List.of(), load));
        assertEquals("Workload for technician T1 cannot be negative: -1.", error.getMessage());
    }

    @Test
    void nullKeyDoesNotStopTheNegativeCheckOfLaterEntries() {
        Map<Technician, Integer> load = new HashMap<>();
        load.put(null, 0);
        load.put(a, -1);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> strategy.selectTechnician(incident, List.of(a), load));
        assertEquals("Workload for technician T1 cannot be negative: -1.", error.getMessage());
    }

    @Test
    void extraMapEntryForNonCandidateIsNeverChosen() {
        Technician outsider = technician("T9", "Outsider", IncidentCategory.NETWORK);
        Map<Technician, Integer> load = new HashMap<>();
        load.put(outsider, 0);
        load.put(a, 5);

        assertSame(a, strategy.selectTechnician(incident, List.of(a), load).orElseThrow());
    }

    @Test
    void validationRunsInTheAgreedOrder() {
        Map<Technician, Integer> negative = new HashMap<>();
        negative.put(a, -1);

        // null incident is reported before a null list; a null list before a negative map entry
        assertEquals("Incident must not be null.", assertThrows(IllegalArgumentException.class,
                () -> strategy.selectTechnician(null, null, negative)).getMessage());
        assertEquals("Candidate list must not be null.", assertThrows(IllegalArgumentException.class,
                () -> strategy.selectTechnician(incident, null, negative)).getMessage());
    }

    // ---------- read-only behaviour ----------

    @Test
    void inputsAreNotChangedAndRepeatingGivesSameResult() {
        List<Technician> candidates = new ArrayList<>(List.of(a, b, c, d));
        Map<Technician, Integer> load = fixtureLoads();
        List<Technician> listBefore = new ArrayList<>(candidates);
        Map<Technician, Integer> loadBefore = new HashMap<>(load);

        Technician first = strategy.selectTechnician(incident, candidates, load).orElseThrow();
        Technician second = strategy.selectTechnician(incident, candidates, load).orElseThrow();

        assertSame(b, first);
        assertSame(first, second);
        assertEquals(listBefore, candidates);
        assertEquals(loadBefore, load);
        assertTrue(a.isAvailable() && b.isAvailable() && c.isAvailable());
        assertFalse(d.isAvailable());
        assertEquals(Set.of(IncidentCategory.SOFTWARE), b.getSkills());
        assertEquals(IncidentCategory.NETWORK, incident.getCategory());
        assertEquals(IncidentStatus.NEW, incident.getStatus());
        assertNull(incident.getAssignedTechnician());
    }

    @Test
    void nameIsStable() {
        assertEquals("LoadBasedStrategy", strategy.getName());
    }
}
