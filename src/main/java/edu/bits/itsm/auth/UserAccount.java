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
}