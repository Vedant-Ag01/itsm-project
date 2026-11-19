package edu.bits.itsm.notification;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// Wraps a channel that talks to ONE shared place (like a Telegram group) so that an event
// with several recipients is delivered once, not once per recipient.
// The same text arriving again within a few seconds counts as the same event.
public class DeduplicatingChannel implements NotificationChannel {

    private static final Duration DEFAULT_WINDOW = Duration.ofSeconds(5);

    private final NotificationChannel delegate;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Instant> recentlySent = new HashMap<>();

    public DeduplicatingChannel(NotificationChannel delegate) {
        this(delegate, DEFAULT_WINDOW, Clock.systemUTC());
    }

    public DeduplicatingChannel(NotificationChannel delegate, Duration window, Clock clock) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate channel must not be null");
        }
        this.delegate = delegate;
        this.window = window;
        this.clock = clock;
    }

    @Override
    public synchronized void send(String recipient, String subject, String message) {
        Instant now = clock.instant();
        recentlySent.values().removeIf(sentAt -> sentAt.plus(window).isBefore(now));

        String key = subject + "\n" + message;
        if (recentlySent.containsKey(key)) {
            return;   // the same event, already delivered for another recipient
        }
        recentlySent.put(key, now);
        delegate.send(recipient, subject, message);
    }

    @Override
    public String getChannelName() {
        return delegate.getChannelName();
    }
}
