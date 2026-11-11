package edu.bits.itsm.api.dto;

// For actions that need a written note: resolve and close.
// Who is acting is the logged-in user, so there is no "actor" any more.
public record NoteActionRequest(String note) {
}