package edu.bits.itsm.auth;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

// The rules for creating users and switching them on and off.
@Service
public class UserManagementService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserAccountTable users;

    public UserManagementService(UserAccountTable users) {
        this.users = users;
    }

    // Anyone can register, but only as an EMPLOYEE.
    public UserAccount register(String name, String email, String department, String password) {
        return create(name, email, department, password, Role.EMPLOYEE);
    }

    // An admin can create EMPLOYEE or ADMIN accounts.
    // (Technicians are set up in AppConfig because the assignment rules need their skills.)
    public UserAccount createByAdmin(String name, String email, String department,
                                     String password, Role role) {
        if (role == null) {
            throw new IllegalArgumentException("A role is required (EMPLOYEE or ADMIN)");
        }
        if (role == Role.TECHNICIAN) {
            throw new IllegalArgumentException(
                    "Technician accounts need skills and are set up by the development team, "
                            + "not through this screen");
        }
        return create(name, email, department, password, role);
    }

    // Anything left null keeps its current value.
    public UserAccount update(String actingAdminId, String userId, String name,
                              String department, Boolean active) {
        UserAccount account = users.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("Unknown user id " + userId));

        if (active != null && !active && account.id.equals(actingAdminId)) {
            throw new IllegalArgumentException("You cannot switch off your own account");
        }
        if (name != null) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("Name cannot be empty");
            }
            account.name = name.trim();
        }
        if (department != null) {
            account.department = department.isBlank() ? "General" : department.trim();
        }
        if (active != null) {
            account.active = active;
        }
        return users.save(account);
    }

    public List<UserAccount> all() {
        return users.findAll();
    }

    private UserAccount create(String name, String email, String department,
                               String password, Role role) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        String cleanEmail = normalizeEmail(email);
        if (!EMAIL.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Please enter a valid email address");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        if (users.findByEmail(cleanEmail).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        UserAccount account = new UserAccount();
        account.id = "U" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        account.name = name.trim();
        account.email = cleanEmail;
        account.department = (department == null || department.isBlank())
                ? "General" : department.trim();
        account.role = role;
        account.passwordSalt = PasswordHasher.newSalt();
        account.passwordHash = PasswordHasher.hash(password, account.passwordSalt);
        account.active = true;
        return users.save(account);
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
