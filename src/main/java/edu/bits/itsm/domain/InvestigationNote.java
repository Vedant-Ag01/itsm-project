package edu.bits.itsm.domain;

import java.time.LocalDateTime;

// One note a technician (or anyone) adds while investigating.
// Immutable: once written, it never changes, which is what you want for a history.
public class InvestigationNote {

    private final User author;
    private final String text;
    private final LocalDateTime createdAt;

    public InvestigationNote(User author, String text) {
        if (author == null) {
            throw new InvalidIncidentException("A note must have an author");
        }
        if (text == null || text.isBlank()) {
            throw new InvalidIncidentException("A note cannot be empty");
        }
        this.author = author;
        this.text = text;
        this.createdAt = LocalDateTime.now();
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