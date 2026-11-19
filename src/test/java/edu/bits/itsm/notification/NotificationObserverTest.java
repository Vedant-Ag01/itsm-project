package edu.bits.itsm.notification;

import edu.bits.itsm.domain.IncidentStatus;
import edu.bits.itsm.event.IncidentEvent;
import edu.bits.itsm.event.IncidentEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationObserverTest {

    private NotificationObserver notificationObserver;
    private InAppChannel inAppChannel;
    private SimulatedEmailChannel emailChannel;

    @BeforeEach
    void setUp() {
        RecipientResolver stubResolver = event -> List.of("employee@bits.edu", "tech@bits.edu");
        notificationObserver = new NotificationObserver(stubResolver);

        inAppChannel = new InAppChannel();
        emailChannel = new SimulatedEmailChannel();

        notificationObserver.addChannel(inAppChannel);
        notificationObserver.addChannel(emailChannel);
    }

    @Test
    void testNotificationDeliveredToAllChannelsAndRecipients() {
        IncidentEvent event = new IncidentEvent(
                "INC1024",
                IncidentEventType.ASSIGNED,
                "ADMIN",
                IncidentStatus.NEW,
                IncidentStatus.ASSIGNED,
                "Assigned to technician",
                LocalDateTime.now()
        );

        notificationObserver.onEvent(event);

        assertEquals(1, inAppChannel.getNotificationsForUser("employee@bits.edu").size());
        assertEquals(1, inAppChannel.getNotificationsForUser("tech@bits.edu").size());
        assertEquals(2, emailChannel.getSentEmails().size());
    }

    @Test
    void testAddingNewChannelRequiresNoCodeChangesInObserver() {
        class DummySMSChannel implements NotificationChannel {
            int sentCount = 0;
            @Override public void send(String r, String s, String m) { sentCount++; }
            @Override public String getChannelName() { return "SMS"; }
        }

        DummySMSChannel smsChannel = new DummySMSChannel();
        notificationObserver.addChannel(smsChannel);

        IncidentEvent event = new IncidentEvent(
                "INC1024",
                IncidentEventType.RESOLVED,
                "TECH1",
                IncidentStatus.IN_PROGRESS,
                IncidentStatus.RESOLVED,
                "Password reset completed",
                LocalDateTime.now()
        );

        notificationObserver.onEvent(event);
        assertEquals(2, smsChannel.sentCount);
    }
}