package edu.bits.itsm.duplicate;

import edu.bits.itsm.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DuplicateDetectorTest {

    private final DuplicateDetector detector = new SimilarityDuplicateDetector();
    private final Employee emp = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private final ItService paymentService = new ItService("Payment Portal", BusinessCriticality.HIGH);

    private Incident buildIncident(String id, String title, String desc, IncidentCategory cat, IncidentStatus status) {
        return Incident.restore(
                id, emp, title, desc, paymentService, Impact.MEDIUM, Urgency.MEDIUM, 1,
                LocalDateTime.now(), status, cat, Priority.P2,
                null, null, null, null, null, null, List.of(), null
        );
    }

    @Test
    void testDetectsSimilarPaymentIncidents() {
        Incident existing1 = buildIncident("INC1", "Payment application unavailable", "Users get error screen", IncidentCategory.APPLICATION, IncidentStatus.NEW);
        Incident existing2 = buildIncident("INC2", "Payment transaction failing", "Transactions broken on checkout", IncidentCategory.APPLICATION, IncidentStatus.NEW);
        Incident existing3 = buildIncident("INC3", "Printer out of paper", "Library printer empty", IncidentCategory.HARDWARE, IncidentStatus.NEW);

        Incident incoming = buildIncident("INC4", "Unable to make payment", "Payment app is down", IncidentCategory.APPLICATION, IncidentStatus.NEW);

        List<DuplicateMatch> matches = detector.findSimilar(incoming, List.of(existing1, existing2, existing3));

        assertEquals(2, matches.size());
        assertTrue(matches.stream().anyMatch(m -> m.other().getId().equals("INC1")));
        assertTrue(matches.stream().anyMatch(m -> m.other().getId().equals("INC2")));
        assertFalse(matches.stream().anyMatch(m -> m.other().getId().equals("INC3")));
    }

    @Test
    void testIgnoresClosedIncidents() {
        Incident closedIncident = buildIncident("INC1", "Payment application unavailable", "Down", IncidentCategory.APPLICATION, IncidentStatus.CLOSED);
        Incident incoming = buildIncident("INC2", "Payment application down", "Failing", IncidentCategory.APPLICATION, IncidentStatus.NEW);

        List<DuplicateMatch> matches = detector.findSimilar(incoming, List.of(closedIncident));

        assertTrue(matches.isEmpty());
    }
}