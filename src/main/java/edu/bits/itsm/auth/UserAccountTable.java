package edu.bits.itsm.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountTable extends JpaRepository<UserAccount, String> {

    // Spring builds the SQL from the method name: "find a user by email"
    Optional<UserAccount> findByEmail(String email);
}