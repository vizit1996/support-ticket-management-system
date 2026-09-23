package com.supportticket.ticket.domain;

import java.util.UUID;

public final class TicketNotMutableException extends RuntimeException {

    public static final String PROBLEM_CODE = "ticket-not-mutable";

    private final UUID ticketId;
    private final TicketStatus status;

    public TicketNotMutableException(UUID ticketId, TicketStatus status) {
        super("Ticket %s cannot be modified in status %s.".formatted(ticketId, status));
        this.ticketId = ticketId;
        this.status = status;
    }

    public UUID ticketId() {
        return ticketId;
    }

    public TicketStatus status() {
        return status;
    }
}
