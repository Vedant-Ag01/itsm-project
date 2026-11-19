package edu.bits.itsm.api.dto;

import edu.bits.itsm.auth.UserAccount;

public record UserResponse(
        String id,
        String name,
        String email,
        String department,
        String role,
        boolean active) {

    public static UserResponse from(UserAccount account) {
        return new UserResponse(account.id, account.name, account.email,
                account.department, account.role.name(), account.isActive());
    }
}
