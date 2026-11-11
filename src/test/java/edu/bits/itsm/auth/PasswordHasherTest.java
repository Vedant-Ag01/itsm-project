package edu.bits.itsm.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void samePasswordAndSaltGiveTheSameHash() {
        String salt = PasswordHasher.newSalt();

        String first = PasswordHasher.hash("password123", salt);
        String second = PasswordHasher.hash("password123", salt);

        assertEquals(first, second);
    }

    @Test
    void differentSaltsGiveDifferentHashes() {
        String first = PasswordHasher.hash("password123", PasswordHasher.newSalt());
        String second = PasswordHasher.hash("password123", PasswordHasher.newSalt());

        assertNotEquals(first, second);
    }

    @Test
    void matchesAcceptsTheRightPasswordAndRejectsAWrongOne() {
        String salt = PasswordHasher.newSalt();
        String stored = PasswordHasher.hash("password123", salt);

        assertTrue(PasswordHasher.matches("password123", salt, stored));
        assertFalse(PasswordHasher.matches("wrong", salt, stored));
    }

    @Test
    void theHashIsNotThePasswordItself() {
        String salt = PasswordHasher.newSalt();

        String hash = PasswordHasher.hash("password123", salt);

        assertNotEquals("password123", hash);
    }
}