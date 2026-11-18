package edu.bits.itsm.notification;

import java.util.*;

public class InAppChannel implements NotificationChannel {

    public record InAppNotification(String recipient, String subject, String message, boolean isRead) { }

    // Maps recipient username/ID to their list of notifications
    private final Map<String, List<InAppNotification>> userNotifications = new HashMap<>();

    @Override
    public synchronized void send(String recipient, String subject, String message) {
        userNotifications
                .computeIfAbsent(recipient, k -> new ArrayList<>())
                .add(new InAppNotification(recipient, subject, message, false));
    }

    @Override
    public String getChannelName() {
        return "IN_APP";
    }

    public synchronized List<InAppNotification> getNotificationsForUser(String recipient) {
        return new ArrayList<>(userNotifications.getOrDefault(recipient, List.of()));
    }
}