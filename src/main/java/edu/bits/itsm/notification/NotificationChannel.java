package edu.bits.itsm.notification;

public interface NotificationChannel {
    void send(String recipient, String subject, String message);

    String getChannelName();
}