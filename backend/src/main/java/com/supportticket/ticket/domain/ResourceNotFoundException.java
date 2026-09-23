package com.supportticket.ticket.domain;

import java.util.UUID;

public final class ResourceNotFoundException extends RuntimeException {

    public static final String TICKET_NOT_FOUND = "ticket-not-found";

    private final String problemCode;

    private ResourceNotFoundException(String problemCode, String detail) {
        super(detail);
        this.problemCode = problemCode;
    }

    public static ResourceNotFoundException ticket(UUID id) {
        return new ResourceNotFoundException(TICKET_NOT_FOUND, "Ticket %s was not found.".formatted(id));
    }

    public String problemCode() {
        return problemCode;
    }
}
