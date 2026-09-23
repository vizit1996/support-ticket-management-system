package com.supportticket.ticket.domain;

/**
 * Ticket lifecycle statuses. Transition rules are the matrix in {@code spec/state-machine.md}.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    /**
     * @return {@code true} only for the five legal pairs; self-transitions are illegal.
     */
    public boolean canTransitionTo(TicketStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case OPEN -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == RESOLVED || target == CANCELLED;
            case RESOLVED -> target == CLOSED;
            case CLOSED, CANCELLED -> false;
        };
    }

    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }

    /** Field updates and comments are allowed only in non-terminal statuses. */
    public boolean allowsMutation() {
        return !isTerminal();
    }
}
