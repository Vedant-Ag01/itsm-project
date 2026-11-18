package edu.bits.itsm.event;

import java.util.ArrayList;
import java.util.List;

public class IncidentEventPublisher {
    private final List<IncidentObserver> observers = new ArrayList<>();

    public void subscribe(IncidentObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unsubscribe(IncidentObserver observer) {
        observers.remove(observer);
    }

    public void publish(IncidentEvent event) {
        for (IncidentObserver observer : observers) {
            try {
                observer.onEvent(event);   //each observer in observer list deals with "event" using their own method
            } catch (Exception e) {
                // Prevent one broken observer from halting event delivery to others
                System.err.println("Error notifying observer " + observer.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
}