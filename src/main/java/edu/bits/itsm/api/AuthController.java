package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.LoginRequest;
import edu.bits.itsm.api.dto.LoginResponse;
import edu.bits.itsm.api.dto.RegisterRequest;
import edu.bits.itsm.auth.AuthService;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.UserAccount;
import edu.bits.itsm.auth.UserManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserManagementService userService;

    public AuthController(AuthService authService, UserManagementService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());
        return toResponse(result);
    }

    // Open to everyone (no login needed). Always creates an EMPLOYEE,
    // then logs the new user straight in.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@RequestBody RegisterRequest request) {
        userService.register(request.name(), request.email(), request.department(),
                request.password());
        return toResponse(authService.login(request.email(), request.password()));
    }

    // Who am I? Handy for the React app after a page reload.
    @GetMapping("/me")
    public LoginResponse me() {
        UserAccount user = CurrentUser.get();
        return new LoginResponse(CurrentUser.token(), user.id, user.name, user.email,
                user.role.name(), user.department);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        authService.logout(CurrentUser.token());
    }

    private LoginResponse toResponse(AuthService.LoginResult result) {
        UserAccount user = result.user();
        return new LoginResponse(result.token(), user.id, user.name, user.email,
                user.role.name(), user.department);
    }
}
