package edu.bits.itsm.notification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InAppChannel implements NotificationChannel {

    public record InAppNotification(long id, String recipient, String subject, String message,
                                    LocalDateTime createdAt, boolean read) { }

    // Maps recipient (email) to their list of notifications, oldest first
    private final Map<String, List<InAppNotification>> userNotifications = new HashMap<>();
    private long nextId = 1;

    @Override
    public synchronized void send(String recipient, String subject, String message) {
        userNotifications
                .computeIfAbsent(recipient, k -> new ArrayList<>())
                .add(new InAppNotification(nextId++, recipient, subject, message,
                        LocalDateTime.now(), false));
    }

    @Override
    public String getChannelName() {
        return "IN_APP";
    }

    // Newest first
    public synchronized List<InAppNotification> getNotificationsForUser(String recipient) {
        List<InAppNotification> copy =
                new ArrayList<>(userNotifications.getOrDefault(recipient, List.of()));
        java.util.Collections.reverse(copy);
        return copy;
    }

    public synchronized int unreadCount(String recipient) {
        int count = 0;
        for (InAppNotification n : userNotifications.getOrDefault(recipient, List.of())) {
            if (!n.read()) {
                count++;
            }
        }
        return count;
    }

    // Only the owner's notifications are touched, so nobody can mark someone else's
    public synchronized void markRead(String recipient, long id) {
        List<InAppNotification> list = userNotifications.get(recipient);
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            InAppNotification n = list.get(i);
            if (n.id() == id && !n.read()) {
                list.set(i, new InAppNotification(n.id(), n.recipient(), n.subject(),
                        n.message(), n.createdAt(), true));
            }
        }
    }

    public synchronized void markAllRead(String recipient) {
        List<InAppNotification> list = userNotifications.get(recipient);
        if (list == null) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            InAppNotification n = list.get(i);
            if (!n.read()) {
                list.set(i, new InAppNotification(n.id(), n.recipient(), n.subject(),
                        n.message(), n.createdAt(), true));
            }
        }
    }
}