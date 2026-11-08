package edu.bits.itsm.api.dto;

// For actions that need a written note: resolve and close
public record NoteActionRequest(String actor, String note) {
}