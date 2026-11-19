package edu.bits.itsm.audit;

import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.event.IncidentEvent;
import edu.bits.itsm.event.IncidentEventPublisher;
import edu.bits.itsm.event.IncidentEventType;
import edu.bits.itsm.event.IncidentObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AuditLogTest {

    private IncidentEventPublisher publisher;
    private AuditRepository repository;
    private AuditLogObserver auditObserver;

    @BeforeEach
    void setUp() {
        publisher = new IncidentEventPublisher();
        repository = new InMemoryAuditRepository();
        auditObserver = new AuditLogObserver(repository);
        publisher.subscribe(auditObserver);
    }

    @Test
    void testEventCreatesAuditRecord() {
        IncidentEvent event = new IncidentEvent(
                "INC1024",
                IncidentEventType.CREATED,
                "E102",
                null,
                IncidentStatus.NEW,
                "Software crash reported",
                LocalDateTime.now()
        );

        publisher.publish(event);

        List<AuditRecord> records = repository.findByIncident("INC1024");
        assertEquals(1, records.size());
        assertEquals("E102", records.get(0).actor());
        assertEquals(IncidentEventType.CREATED, records.get(0).eventType());
    }

    @Test
    void testFailingObserverDoesNotBlockAuditLogging() {
        IncidentObserver brokenObserver = evt -> {
            throw new RuntimeException("Database offline!");
        };

        publisher.subscribe(brokenObserver);

        IncidentEvent event = new IncidentEvent(
                "INC1025",
                IncidentEventType.ASSIGNED,
                "ADMIN",
                IncidentStatus.NEW,
                IncidentStatus.ASSIGNED,
                "Assigned to Tech T1",
                LocalDateTime.now()
        );

        assertDoesNotThrow(() -> publisher.publish(event));
        assertEquals(1, repository.findByIncident("INC1025").size());
    }
}