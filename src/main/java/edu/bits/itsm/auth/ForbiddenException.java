package edu.bits.itsm.auth;

// The user is logged in but their role is not allowed to do this
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}