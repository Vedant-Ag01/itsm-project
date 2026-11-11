package edu.bits.itsm.domain;

import java.time.LocalDateTime;

// One note a technician (or anyone) adds while investigating.
// Immutable: once written, it never changes, which is what you want for a history.
public class InvestigationNote {

    private final User author;
    private final String text;
    private final LocalDateTime createdAt;

    public InvestigationNote(User author, String text) {
        this(author, text, LocalDateTime.now());
    }

    private InvestigationNote(User author, String text, LocalDateTime createdAt) {
        if (author == null) {
            throw new InvalidIncidentException("A note must have an author");
        }
        if (text == null || text.isBlank()) {
            throw new InvalidIncidentException("A note cannot be empty");
        }
        this.author = author;
        this.text = text;
        this.createdAt = createdAt;
    }

    // For the database layer only: rebuilds a saved note with its original time
    public static InvestigationNote restore(User author, String text, LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new InvalidIncidentException("A saved note must have its original time");
        }
        return new InvestigationNote(author, text, createdAt);
    }

    public User getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}