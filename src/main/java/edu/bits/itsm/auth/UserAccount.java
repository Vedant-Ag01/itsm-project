package edu.bits.itsm.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A person who can log in. Only a data holder for the database.
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    public String id;

    public String name;

    @Column(unique = true)
    public String email;

    public String department;

    @Enumerated(EnumType.STRING)
    public Role role;

    // the password is never stored, only a salted hash of it
    public String passwordSalt;
    public String passwordHash;

    // false = the account is switched off and cannot log in.
    // It is a nullable Boolean so rows that existed before this column was added
    // (value null) simply count as active.
    public Boolean active = true;

    // Only used for technicians: their skills as category names, e.g. "NETWORK,HARDWARE".
    // It is null for everyone else.
    public String skills;

    public boolean isActive() {
        return active == null || active;
    }
}
