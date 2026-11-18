package edu.bits.itsm.notification;

import edu.bits.itsm.event.IncidentEvent;
import edu.bits.itsm.event.IncidentObserver;

import java.util.ArrayList;
import java.util.List;

public class NotificationObserver implements IncidentObserver {

    private final List<NotificationChannel> channels = new ArrayList<>();
    private final RecipientResolver recipientResolver;

    public NotificationObserver(RecipientResolver recipientResolver) {
        this.recipientResolver = recipientResolver;
    }

    public void addChannel(NotificationChannel channel) {
        if (channel != null && !channels.contains(channel)) {
            channels.add(channel);
        }
    }

    @Override
    public void onEvent(IncidentEvent event) {
        List<String> recipients = recipientResolver.recipientsFor(event);
        if (recipients.isEmpty()) {
            return;
        }

        NotificationFactory.NotificationContent content = NotificationFactory.createMessage(event);

        // Send to every recipient across every registered channel
        for (String recipient : recipients) {
            for (NotificationChannel channel : channels) {
                try {
                    channel.send(recipient, content.subject(), content.body());
                } catch (Exception e) {
                    System.err.println("Failed to send via " + channel.getChannelName() + " to " + recipient + ": " + e.getMessage());
                }
            }
        }
    }
}