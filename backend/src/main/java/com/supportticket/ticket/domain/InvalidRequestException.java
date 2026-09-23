package com.supportticket.ticket.domain;

import java.util.List;

public final class InvalidRequestException extends RuntimeException {

    public static final String VALIDATION = "validation";
    public static final String INVALID_STATUS = "invalid-status";

    private final String problemCode;
    private final List<FieldError> errors;

    public InvalidRequestException(String problemCode, String detail, List<FieldError> errors) {
        super(detail);
        this.problemCode = problemCode;
        this.errors = List.copyOf(errors);
    }

    public static InvalidRequestException validation(String detail, List<FieldError> errors) {
        return new InvalidRequestException(VALIDATION, detail, errors);
    }

    public String problemCode() {
        return problemCode;
    }

    public List<FieldError> errors() {
        return errors;
    }

    public record FieldError(String field, String message) {
    }
}
