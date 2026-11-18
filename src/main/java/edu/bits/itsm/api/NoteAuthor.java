package edu.bits.itsm.api;

import edu.bits.itsm.auth.UserAccount;
import edu.bits.itsm.domain.User;

// A domain User built from the logged-in account, so it can be the author of a note
public class NoteAuthor extends User {

    private final String role;

    public NoteAuthor(UserAccount account) {
        super(account.id, account.name, account.email);
        this.role = account.role.name();
    }

    @Override
    public String getRoleName() {
        return role;
    }
}