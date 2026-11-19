package edu.bits.itsm.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

// One investigation note as it is stored in the database
@Embeddable
public class NoteData {

    public String authorId;
    public String authorName;
    public String authorEmail;
    public String authorRole;

    @Column(length = 2000)
    public String noteText;

    public LocalDateTime createdAt;

    // NOTE, INFO_REQUEST or INFO_RESPONSE. Empty in rows saved before this column existed.
    public String noteType;
}