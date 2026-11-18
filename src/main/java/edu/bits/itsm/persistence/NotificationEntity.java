package edu.bits.itsm.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

// One row per in-app notification
@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String recipient;

    public String subject;

    @Column(length = 4000)
    public String message;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "is_read")
    public boolean read;

    public NotificationEntity() {
    }
}