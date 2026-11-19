package edu.bits.itsm.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicatingChannelTest {

    // a clock the test can move forward
    private static class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-11-19T12:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        public Clock withZone(ZoneId zone) {
            return this;
        }

        public Instant instant() {
            return now;
        }
    }

    private static class Recording implements NotificationChannel {
        final List<String> sent = new ArrayList<>();

        public void send(String recipient, String subject, String message) {
            sent.add(recipient + "|" + subject);
        }

        public String getChannelName() {
            return "TEST";
        }
    }

    private MovableClock clock;
    private Recording inner;
    private DeduplicatingChannel channel;

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
        inner = new Recording();
        channel = new DeduplicatingChannel(inner, Duration.ofSeconds(5), clock);
    }

    @Test
    void oneEventForThreeRecipientsIsDeliveredOnce() {
        channel.send("admin@bits.edu", "[ASSIGNED] INC1", "Assigned to Ravi");
        channel.send("asha@bits.edu", "[ASSIGNED] INC1", "Assigned to Ravi");
        channel.send("ravi@bits.edu", "[ASSIGNED] INC1", "Assigned to Ravi");

        assertEquals(1, inner.sent.size());
        assertEquals("admin@bits.edu|[ASSIGNED] INC1", inner.sent.get(0));
    }

    @Test
    void differentEventsAreAllDelivered() {
        channel.send("a@bits.edu", "[ASSIGNED] INC1", "Assigned");
        channel.send("a@bits.edu", "[RESOLVED] INC1", "Resolved");

        assertEquals(2, inner.sent.size());
    }

    @Test
    void theSameTextMuchLaterIsDeliveredAgain() {
        channel.send("a@bits.edu", "[ESCALATED] INC1", "Escalated to TEAM_LEAD");
        clock.advance(Duration.ofSeconds(30));
        channel.send("a@bits.edu", "[ESCALATED] INC1", "Escalated to TEAM_LEAD");

        assertEquals(2, inner.sent.size());
    }

    @Test
    void reportsTheNameOfTheWrappedChannel() {
        assertEquals("TEST", channel.getChannelName());
    }

    @Test
    void rejectsAMissingChannel() {
        assertThrows(IllegalArgumentException.class, () -> new DeduplicatingChannel(null));
    }
}
