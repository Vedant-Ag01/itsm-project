package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.UserResponse;
import edu.bits.itsm.auth.AllowedRoles;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.Role;
import edu.bits.itsm.auth.UserManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Admin tools for managing user accounts (the list itself is GET /api/admin/users)
@RestController
public class UserAdminController {

    public record CreateUserRequest(String name, String email, String department,
                                    String password, Role role) { }

    public record UpdateUserRequest(String name, String department, Boolean active) { }

    private final UserManagementService userService;

    public UserAdminController(UserManagementService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/admin/users")
    @AllowedRoles({Role.ADMIN})
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.createByAdmin(request.name(), request.email(),
                request.department(), request.password(), request.role()));
    }

    // Change name or department, or switch an account on/off with {"active": false}
    @PutMapping("/api/admin/users/{id}")
    @AllowedRoles({Role.ADMIN})
    public UserResponse update(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.update(CurrentUser.get().id, id,
                request.name(), request.department(), request.active()));
    }
}
