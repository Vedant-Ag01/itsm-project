package edu.bits.itsm.api.dto;

public record LoginResponse(
        String token,
        String userId,
        String name,
        String email,
        String role,
        String department) {
}