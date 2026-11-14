package edu.csuf.sis.gateway;

import edu.csuf.sis.common.NotFoundException;
import edu.csuf.sis.common.PermissionDeniedException;
import edu.csuf.sis.common.RuleViolationException;
import edu.csuf.sis.identity.LoginFailedException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Turns refusals into standard problem responses ({@code application/problem+json}) whose
 * {@code detail} is a sentence the portals show to the user as-is.
 */
@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(RuleViolationException.class)
    ProblemDetail ruleViolation(RuleViolationException refusal) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, refusal.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException missing) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, missing.getMessage());
    }

    @ExceptionHandler(PermissionDeniedException.class)
    ProblemDetail permissionDenied(PermissionDeniedException denied) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, denied.getMessage());
    }

    @ExceptionHandler(LoginFailedException.class)
    ProblemDetail loginFailed(LoginFailedException failed) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, failed.getMessage());
    }

    /** Domain validation, e.g. a malformed course code or day code. */
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badValue(IllegalArgumentException invalid) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, invalid.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidForm(MethodArgumentNotValidException invalid) {
        String detail = invalid.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail + ".");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException unreadable) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The request body is missing or is not valid JSON (dates look like 2027-01-25, times like 13:00).");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail wrongType(MethodArgumentTypeMismatchException mismatch) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "'" + mismatch.getValue() + "' is not a valid " + mismatch.getName() + ".");
    }

    /** Used by the security filter chain, which runs before any controller exists to throw. */
    static void write(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"title\":\"" + status.getReasonPhrase() + "\",\"status\":" + status.value()
                + ",\"detail\":\"" + detail + "\"}");
    }
}
