package com.supportticket.ticket.web;

import com.supportticket.ticket.domain.InvalidRequestException;
import com.supportticket.ticket.domain.InvalidStateTransitionException;
import com.supportticket.ticket.domain.ResourceNotFoundException;
import com.supportticket.ticket.domain.TicketNotMutableException;
import com.supportticket.ticket.domain.TicketStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String PROBLEM_BASE = "https://api.support-tickets.local/problems/";

    @ExceptionHandler(InvalidStateTransitionException.class)
    ResponseEntity<ProblemDetail> handleIllegalTransition(
            InvalidStateTransitionException ex,
            HttpServletRequest request
    ) {
        ProblemDetail problem = base(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "illegal-ticket-state",
                "Unprocessable Entity",
                ex.getMessage(),
                request
        );
        problem.setProperty("from", ex.from().name());
        problem.setProperty("to", ex.to().name());
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    @ExceptionHandler(TicketNotMutableException.class)
    ResponseEntity<ProblemDetail> handleNotMutable(TicketNotMutableException ex, HttpServletRequest request) {
        ProblemDetail problem = base(
                HttpStatus.UNPROCESSABLE_ENTITY,
                TicketNotMutableException.PROBLEM_CODE,
                "Unprocessable Entity",
                ex.getMessage(),
                request
        );
        return problemResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = base(
                HttpStatus.NOT_FOUND,
                ex.problemCode(),
                "Not Found",
                ex.getMessage(),
                request
        );
        return problemResponse(HttpStatus.NOT_FOUND, problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<Map<String, String>> errors = new java.util.ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.add(fieldError(err.getField(), err.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(err -> errors.add(fieldError(err.getObjectName(), err.getDefaultMessage())));
        return validation(request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(v -> fieldError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return validation(request, errors);
    }

    @ExceptionHandler(InvalidRequestException.class)
    ResponseEntity<ProblemDetail> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request) {
        ProblemDetail problem = base(
                HttpStatus.BAD_REQUEST,
                ex.problemCode(),
                "Bad Request",
                ex.getMessage(),
                request
        );
        if (!ex.errors().isEmpty()) {
            problem.setProperty(
                    "errors",
                    ex.errors().stream().map(e -> fieldError(e.field(), e.message())).toList()
            );
        }
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        if (ex.getRequiredType() == TicketStatus.class) {
            ProblemDetail problem = base(
                    HttpStatus.BAD_REQUEST,
                    InvalidRequestException.INVALID_STATUS,
                    "Bad Request",
                    "status must be a valid TicketStatus.",
                    request
            );
            return problemResponse(HttpStatus.BAD_REQUEST, problem);
        }
        String field = ex.getName();
        String message = ex.getRequiredType() == UUID.class
                ? "must be a valid UUID"
                : "invalid value";
        return validation(request, List.of(fieldError(field, message)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return validation(request, List.of(fieldError("body", "malformed JSON or invalid enum value")));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = base(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "internal",
                "Internal Server Error",
                "An unexpected error occurred.",
                request
        );
        return problemResponse(HttpStatus.INTERNAL_SERVER_ERROR, problem);
    }

    private static ResponseEntity<ProblemDetail> validation(HttpServletRequest request, List<Map<String, String>> errors) {
        ProblemDetail problem = base(
                HttpStatus.BAD_REQUEST,
                InvalidRequestException.VALIDATION,
                "Bad Request",
                "Request validation failed.",
                request
        );
        problem.setProperty("errors", errors);
        return problemResponse(HttpStatus.BAD_REQUEST, problem);
    }

    private static ProblemDetail base(
            HttpStatus status,
            String code,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(PROBLEM_BASE + code));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private static Map<String, String> fieldError(String field, String message) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("field", field);
        error.put("message", message == null ? "invalid" : message);
        return error;
    }

    private static ResponseEntity<ProblemDetail> problemResponse(HttpStatus status, ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
