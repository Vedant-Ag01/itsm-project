package edu.bits.itsm.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// Turns a password into a hash. A hash cannot be turned back into the password,
// so even if someone reads the database they do not see real passwords.
public class PasswordHasher {

    // A random "salt" is mixed in so two users with the same password
    // still get different hashes.
    public static String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static boolean matches(String password, String salt, String expectedHash) {
        return hash(password, salt).equals(expectedHash);
    }
}