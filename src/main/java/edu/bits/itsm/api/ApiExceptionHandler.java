package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.ErrorResponse;
import edu.bits.itsm.domain.IncidentNotFoundException;
import edu.bits.itsm.domain.InvalidIncidentException;
import edu.bits.itsm.domain.InvalidStateTransitionException;
import edu.bits.itsm.domain.TechnicianUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    // Unknown incident id -> 404
    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(IncidentNotFoundException e) {
        return reply(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // Illegal lifecycle move, like closing a NEW incident -> 409
    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> badTransition(InvalidStateTransitionException e) {
        return reply(HttpStatus.CONFLICT, e.getMessage());
    }

    // Nobody free to take the incident -> 409
    @ExceptionHandler(TechnicianUnavailableException.class)
    public ResponseEntity<ErrorResponse> noTechnician(TechnicianUnavailableException e) {
        return reply(HttpStatus.CONFLICT, e.getMessage());
    }

    // Bad input, like an empty title -> 400
    @ExceptionHandler(InvalidIncidentException.class)
    public ResponseEntity<ErrorResponse> badIncident(InvalidIncidentException e) {
        return reply(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badArgument(IllegalArgumentException e) {
        return reply(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Anything we did not expect -> 500, without leaking internal details
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e) {
        e.printStackTrace();
        return reply(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on the server");
    }

    private ResponseEntity<ErrorResponse> reply(HttpStatus status, String message) {
        ErrorResponse body = new ErrorResponse(
                status.value(), status.getReasonPhrase(), message, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}