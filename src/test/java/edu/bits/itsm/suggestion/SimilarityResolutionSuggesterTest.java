package edu.bits.itsm.suggestion;

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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimilarityResolutionSuggesterTest {

    private final Employee reporter = new Employee("E1", "Asha", "asha@bits.edu", "Finance");
    private final ItService paymentService = new ItService("Payment Gateway", BusinessCriticality.HIGH);
    private final ItService printerService = new ItService("Printing", BusinessCriticality.LOW);
    private final SimilarityResolutionSuggester suggester = new SimilarityResolutionSuggester();

    private Incident buildIncident(String id, String title, String desc, ItService service,
                                   IncidentCategory category, IncidentStatus status, String resolutionNote) {
        return Incident.restore(
                id, reporter, title, desc, service, Impact.MEDIUM, Urgency.MEDIUM, 10,
                LocalDateTime.of(2026, 11, 18, 10, 0),
                status, category, Priority.P2, null,
                null, status == IncidentStatus.RESOLVED ? LocalDateTime.of(2026, 11, 18, 11, 0) : null,
                null, resolutionNote, null, List.of(), null
        );
    }

    @Test
    void suggestsMatchingResolvedIncidentsSortedBySimilarityAndRespectsLimit() {
        Incident incoming = buildIncident(
                "INC200", "Payment gateway unavailable", "Transactions failing at checkout",
                paymentService, IncidentCategory.APPLICATION, IncidentStatus.NEW, null
        );

        Incident exactMatch = buildIncident(
                "INC101", "Payment gateway down", "Checkout transactions failing with timeout",
                paymentService, IncidentCategory.APPLICATION, IncidentStatus.RESOLVED,
                "Restarted payment worker pool and cleared stuck queue"
        );

        Incident partialMatch = buildIncident(
                "INC102", "Payment portal error", "Users see error during payment",
                paymentService, IncidentCategory.APPLICATION, IncidentStatus.CLOSED,
                "Renewed TLS certificate on payment endpoint"
        );

        Incident unrelated = buildIncident(
                "INC103", "Floor 3 printer offline", "Paper jam in tray 2",
                printerService, IncidentCategory.HARDWARE, IncidentStatus.RESOLVED,
                "Cleared paper jam and reset spooler"
        );

        List<ResolutionSuggestion> results = suggester.suggest(
                incoming, List.of(unrelated, partialMatch, exactMatch), 2
        );

        assertEquals(2, results.size());
        assertEquals("INC101", results.get(0).incidentId());
        assertEquals("Restarted payment worker pool and cleared stuck queue", results.get(0).resolutionNote());
        assertTrue(results.get(0).similarity() >= results.get(1).similarity());
    }

    @Test
    void ignoresOpenIncidentsAndSelfMatches() {
        Incident incoming = buildIncident(
                "INC200", "VPN connection failing", "Cannot connect to campus VPN",
                paymentService, IncidentCategory.NETWORK, IncidentStatus.NEW, null
        );

        Incident stillOpen = buildIncident(
                "INC201", "VPN connection failing", "Cannot connect to campus VPN",
                paymentService, IncidentCategory.NETWORK, IncidentStatus.IN_PROGRESS, "Investigating"
        );

        List<ResolutionSuggestion> results = suggester.suggest(incoming, List.of(incoming, stillOpen), 5);
        assertTrue(results.isEmpty());
    }
}
