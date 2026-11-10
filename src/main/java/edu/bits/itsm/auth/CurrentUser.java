package edu.bits.itsm.auth;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

// Lets any code ask: "who is making the request I am handling right now?"
public class CurrentUser {

    public static final String USER_KEY = "currentUser";
    public static final String TOKEN_KEY = "currentToken";

    public static UserAccount get() {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        Object user = attributes.getAttribute(USER_KEY, RequestAttributes.SCOPE_REQUEST);
        if (user == null) {
            throw new AuthenticationFailedException("Please log in first");
        }
        return (UserAccount) user;
    }

    public static String token() {
        RequestAttributes attributes = RequestContextHolder.currentRequestAttributes();
        Object token = attributes.getAttribute(TOKEN_KEY, RequestAttributes.SCOPE_REQUEST);
        return (String) token;
    }
}