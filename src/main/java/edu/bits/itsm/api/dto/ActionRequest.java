package edu.bits.itsm.api.dto;

// Who is doing the action. Temporary until login exists (V8).
public record ActionRequest(String actor) {
}