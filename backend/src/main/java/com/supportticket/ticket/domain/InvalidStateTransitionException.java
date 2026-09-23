package com.supportticket.ticket.domain;

public final class InvalidStateTransitionException extends RuntimeException {

    private final TicketStatus from;
    private final TicketStatus to;

    public InvalidStateTransitionException(TicketStatus from, TicketStatus to) {
        super("Transition from %s to %s is not allowed.".formatted(from, to));
        this.from = from;
        this.to = to;
    }

    public TicketStatus from() {
        return from;
    }

    public TicketStatus to() {
        return to;
    }
}
