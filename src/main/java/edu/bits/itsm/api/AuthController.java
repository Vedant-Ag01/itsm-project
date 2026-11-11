package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.LoginRequest;
import edu.bits.itsm.api.dto.LoginResponse;
import edu.bits.itsm.auth.AuthService;
import edu.bits.itsm.auth.CurrentUser;
import edu.bits.itsm.auth.UserAccount;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());
        UserAccount user = result.user();
        return new LoginResponse(result.token(), user.id, user.name, user.email,
                user.role.name(), user.department);
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
}