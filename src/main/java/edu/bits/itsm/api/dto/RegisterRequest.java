package edu.bits.itsm.api.dto;

public record RegisterRequest(String name, String email, String department, String password) {
}
