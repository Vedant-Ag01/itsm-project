package edu.bits.itsm.notification;

import java.util.ArrayList;
import java.util.List;

public class SimulatedEmailChannel implements NotificationChannel {

    public record SentEmail(String recipient, String subject, String body) { }

    private final List<SentEmail> sentEmails = new ArrayList<>();

    @Override
    public synchronized void send(String recipient, String subject, String message) {
        SentEmail email = new SentEmail(recipient, subject, message);
        sentEmails.add(email);
        System.out.println("SIMULATED EMAIL SENT -> To: " + recipient + " | Subject: " + subject);
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    public synchronized List<SentEmail> getSentEmails() {
        return new ArrayList<>(sentEmails);
    }
}