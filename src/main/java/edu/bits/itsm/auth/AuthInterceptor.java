package edu.bits.itsm.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

// Runs before every /api request (except login): who is this, and may they do this?
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // Browsers send an OPTIONS "can I call you?" request first (CORS). Let it through.
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = tokenFrom(request);
        Optional<UserAccount> found = authService.findByToken(token);
        if (found.isEmpty()) {
            throw new AuthenticationFailedException("Please log in first");
        }
        UserAccount user = found.get();

        // remember the user for the rest of this request
        request.setAttribute(CurrentUser.USER_KEY, user);
        request.setAttribute(CurrentUser.TOKEN_KEY, token);

        // does the endpoint have an @AllowedRoles label?
        if (handler instanceof HandlerMethod method) {
            AllowedRoles allowed = method.getMethodAnnotation(AllowedRoles.class);
            if (allowed != null) {
                boolean roleIsAllowed = false;
                for (Role role : allowed.value()) {
                    if (role == user.role) {
                        roleIsAllowed = true;
                    }
                }
                if (!roleIsAllowed) {
                    throw new ForbiddenException(
                            "Your role (" + user.role + ") is not allowed to do this");
                }
            }
        }
        return true;
    }

    // The header looks like "Authorization: Bearer <token>"
    private String tokenFrom(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7).trim();
    }
}