package edu.bits.itsm.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Runs once at startup. If there are no users yet, it creates a few demo ones.
@Component
public class DemoUsersLoader implements CommandLineRunner {

    private final UserAccountTable users;

    public DemoUsersLoader(UserAccountTable users) {
        this.users = users;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        // The technician ids T1 and T2 match the technicians in AppConfig
        addUser("A1", "Admin", "admin@bits.edu", "IT", Role.ADMIN);
        addUser("E1", "Asha", "asha@bits.edu", "Finance", Role.EMPLOYEE);
        addUser("T1", "Ravi", "ravi@bits.edu", "IT", Role.TECHNICIAN);
        addUser("T2", "Meera", "meera@bits.edu", "IT", Role.TECHNICIAN);
    }

    private void addUser(String id, String name, String email, String department, Role role) {
        UserAccount account = new UserAccount();
        account.id = id;
        account.name = name;
        account.email = email;
        account.department = department;
        account.role = role;
        account.passwordSalt = PasswordHasher.newSalt();
        // Demo password for everyone. Fine for a college demo.
        account.passwordHash = PasswordHasher.hash("password123", account.passwordSalt);
        users.save(account);
    }
}