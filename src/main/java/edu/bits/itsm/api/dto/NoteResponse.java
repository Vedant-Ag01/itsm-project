package edu.bits.itsm.api.dto;

import edu.bits.itsm.domain.InvestigationNote;

import java.time.LocalDateTime;

public record NoteResponse(
        String authorName,
        String authorRole,
        String text,
        LocalDateTime createdAt,
        String type) {

    public static NoteResponse from(InvestigationNote note) {
        return new NoteResponse(
                note.getAuthor().getName(),
                note.getAuthor().getRoleName(),
                note.getText(),
                note.getCreatedAt(),
                note.getType().name());
    }
}