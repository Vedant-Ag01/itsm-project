package edu.bits.itsm.auth;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    // What a successful login gives back
    public record LoginResult(String token, UserAccount user) {
    }

    private final UserAccountTable users;

    // token -> id of the user who owns it
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    public AuthService(UserAccountTable users) {
        this.users = users;
    }

    public LoginResult login(String email, String password) {
        if (email == null || password == null) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        Optional<UserAccount> found = users.findByEmail(email);
        if (found.isEmpty()) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        UserAccount account = found.get();
        if (!PasswordHasher.matches(password, account.passwordSalt, account.passwordHash)) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        String token = UUID.randomUUID().toString();
        tokens.put(token, account.id);
        return new LoginResult(token, account);
    }

    // Who does this token belong to? Empty if the token is unknown.
    public Optional<UserAccount> findByToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String userId = tokens.get(token);
        if (userId == null) {
            return Optional.empty();
        }
        return users.findById(userId);
    }

    public void logout(String token) {
        if (token != null) {
            tokens.remove(token);
        }
    }
}