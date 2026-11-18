package edu.bits.itsm.api;

import edu.bits.itsm.api.dto.ErrorResponse;
import edu.bits.itsm.auth.AuthenticationFailedException;
import edu.bits.itsm.auth.ForbiddenException;
import edu.bits.itsm.domain.IncidentNotFoundException;
import edu.bits.itsm.domain.InvalidIncidentException;
import edu.bits.itsm.domain.InvalidStateTransitionException;
import edu.bits.itsm.domain.ServiceNotFoundException;
import edu.bits.itsm.domain.SlaConfigurationMissingException;
import edu.bits.itsm.domain.TechnicianOverloadedException;
import edu.bits.itsm.domain.TechnicianUnavailableException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    // Not logged in, or wrong email/password -> 401
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> notLoggedIn(AuthenticationFailedException e) {
        return reply(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    // Logged in, but the role is not allowed -> 403
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> forbidden(ForbiddenException e) {
        return reply(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // Unknown incident id -> 404
    @ExceptionHandler(IncidentNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(IncidentNotFoundException e) {
        return reply(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // The service named in a report is not in the catalogue (or is switched off) -> 400
    @ExceptionHandler(ServiceNotFoundException.class)
    public ResponseEntity<ErrorResponse> noSuchService(ServiceNotFoundException e) {
        return reply(HttpStatus.BAD_REQUEST, e.getMessage());
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

    // The technician already has too many active incidents -> 409
    @ExceptionHandler(TechnicianOverloadedException.class)
    public ResponseEntity<ErrorResponse> overloaded(TechnicianOverloadedException e) {
        return reply(HttpStatus.CONFLICT, e.getMessage());
    }

    // No SLA target saved for a priority -> 409
    @ExceptionHandler(SlaConfigurationMissingException.class)
    public ResponseEntity<ErrorResponse> missingSla(SlaConfigurationMissingException e) {
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

    // The request body is not valid JSON, or has a wrong value like impact "HUGE" -> 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadableBody(HttpMessageNotReadableException e) {
        return reply(HttpStatus.BAD_REQUEST,
                "The request body is missing or not valid. Check the JSON and the allowed values.");
    }

    // A value in the URL has the wrong type, like a date that is not a date -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> wrongType(MethodArgumentTypeMismatchException e) {
        return reply(HttpStatus.BAD_REQUEST, "Invalid value for '" + e.getName() + "'");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> wrongMethod(HttpRequestMethodNotSupportedException e) {
        return reply(HttpStatus.METHOD_NOT_ALLOWED, "This URL does not support " + e.getMethod());
    }

    // A URL that does not exist -> 404
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noSuchUrl(NoResourceFoundException e) {
        return reply(HttpStatus.NOT_FOUND, "There is nothing at this address");
    }

    // The database rejected the data (for example a duplicate) -> 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> dataConflict(DataIntegrityViolationException e) {
        return reply(HttpStatus.CONFLICT, "This conflicts with data that already exists");
    }

    // The database is down or unreachable -> 503, and the app keeps running
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ErrorResponse> databaseDown(Exception e) {
        System.err.println("Database problem: " + e.getMessage());
        return reply(HttpStatus.SERVICE_UNAVAILABLE,
                "The database is temporarily unavailable. Please try again in a moment.");
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
