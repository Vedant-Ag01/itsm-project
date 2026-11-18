package edu.bits.itsm.api;

import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.notification.InAppChannel;
import edu.bits.itsm.persistence.PersistentInAppChannel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// A user's own notifications. The owner is always the logged-in user (by email),
// so nobody can read or change someone else's.
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final PersistentInAppChannel inAppChannel;

    public NotificationController(PersistentInAppChannel inAppChannel) {
        this.inAppChannel = inAppChannel;
    }

    @GetMapping
    public Map<String, Object> mine() {
        String email = CurrentUser.get().email;
        List<InAppChannel.InAppNotification> items = inAppChannel.getNotificationsForUser(email);
        return Map.of(
                "unread", inAppChannel.unreadCount(email),
                "notifications", items);
    }

    @PostMapping("/{id}/read")
    public Map<String, Object> markRead(@PathVariable long id) {
        String email = CurrentUser.get().email;
        inAppChannel.markRead(email, id);
        return Map.of("unread", inAppChannel.unreadCount(email));
    }

    @PostMapping("/read-all")
    public Map<String, Object> markAllRead() {
        String email = CurrentUser.get().email;
        inAppChannel.markAllRead(email);
        return Map.of("unread", inAppChannel.unreadCount(email));
    }
}