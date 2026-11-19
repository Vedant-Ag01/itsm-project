package edu.bits.itsm.persistence;

import edu.bits.itsm.notification.InAppChannel;
import edu.bits.itsm.notification.NotificationChannel;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// The in-app notification channel, but saved in PostgreSQL so it survives a restart.
// It is a NotificationChannel like C's others, so NotificationObserver uses it the same way.
public class PersistentInAppChannel implements NotificationChannel {

    private final NotificationTable table;

    public PersistentInAppChannel(NotificationTable table) {
        this.table = table;
    }

    @Override
    @Transactional
    public void send(String recipient, String subject, String message) {
        NotificationEntity entity = new NotificationEntity();
        entity.recipient = recipient;
        entity.subject = subject;
        entity.message = message;
        entity.createdAt = LocalDateTime.now();
        entity.read = false;
        table.save(entity);
    }

    @Override
    public String getChannelName() {
        return "IN_APP";
    }

    // Newest first
    @Transactional(readOnly = true)
    public List<InAppChannel.InAppNotification> getNotificationsForUser(String recipient) {
        List<InAppChannel.InAppNotification> result = new ArrayList<>();
        for (NotificationEntity e : table.findByRecipientOrderByIdDesc(recipient)) {
            result.add(new InAppChannel.InAppNotification(e.id, e.recipient, e.subject,
                    e.message, e.createdAt, e.read));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public int unreadCount(String recipient) {
        return (int) table.countByRecipientAndReadFalse(recipient);
    }

    // Only the owner's notification is found, so nobody can mark someone else's
    @Transactional
    public void markRead(String recipient, long id) {
        table.findByIdAndRecipient(id, recipient).ifPresent(e -> {
            e.read = true;
            table.save(e);
        });
    }

    @Transactional
    public void markAllRead(String recipient) {
        for (NotificationEntity e : table.findByRecipientAndReadFalse(recipient)) {
            e.read = true;
            table.save(e);
        }
    }
}