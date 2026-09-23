package com.supportticket.ticket.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-22T18:00:00Z"), ZoneOffset.UTC);

    private static Ticket newOpenTicket() {
        return Ticket.open("Title", "Description", Priority.HIGH, "reporter-1", null, CLOCK);
    }

    @Test
    void should_start_in_open_status() {
        Ticket ticket = newOpenTicket();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getResolvedAt()).isNull();
        assertThat(ticket.getClosedAt()).isNull();
        assertThat(ticket.getVersion()).isZero();
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED",
            "OPEN, CANCELLED",
            "IN_PROGRESS, CANCELLED"
    })
    void should_apply_legal_transitionTo(TicketStatus from, TicketStatus to) {
        Ticket ticket = ticketIn(from);
        ticket.transitionTo(to, CLOCK);
        assertThat(ticket.getStatus()).isEqualTo(to);
        assertThat(ticket.getUpdatedAt()).isEqualTo(java.time.OffsetDateTime.parse("2026-09-22T18:00:00Z"));
    }

    @ParameterizedTest
    @CsvSource({
            "CLOSED, OPEN",
            "RESOLVED, OPEN",
            "CANCELLED, OPEN",
            "OPEN, CLOSED",
            "RESOLVED, RESOLVED"
    })
    void should_throw_when_transitionTo_is_illegal(TicketStatus from, TicketStatus to) {
        Ticket ticket = ticketIn(from);
        assertThatThrownBy(() -> ticket.transitionTo(to, CLOCK))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
        assertThat(ticket.getStatus()).isEqualTo(from);
    }

    @Test
    void should_set_resolvedAt_when_entering_resolved() {
        Ticket ticket = ticketIn(TicketStatus.IN_PROGRESS);
        ticket.transitionTo(TicketStatus.RESOLVED, CLOCK);
        assertThat(ticket.getResolvedAt()).isEqualTo(java.time.OffsetDateTime.parse("2026-09-22T18:00:00Z"));
        assertThat(ticket.getClosedAt()).isNull();
    }

    @Test
    void should_set_closedAt_when_entering_closed_or_cancelled() {
        Ticket closed = ticketIn(TicketStatus.RESOLVED);
        closed.transitionTo(TicketStatus.CLOSED, CLOCK);
        assertThat(closed.getClosedAt()).isNotNull();

        Ticket cancelled = newOpenTicket();
        cancelled.transitionTo(TicketStatus.CANCELLED, CLOCK);
        assertThat(cancelled.getClosedAt()).isNotNull();
    }

    @Test
    void should_associate_comment_with_ticket() {
        Ticket ticket = newOpenTicket();
        Comment comment = ticket.addComment("agent-1", "Looking into this.", CLOCK);
        assertThat(comment.getTicket()).isSameAs(ticket);
        assertThat(ticket.getComments()).containsExactly(comment);
    }

    @Test
    void should_reject_field_update_and_comment_when_terminal() {
        Ticket ticket = ticketIn(TicketStatus.CLOSED);
        assertThatThrownBy(() -> ticket.applyFieldUpdate("x", null, null, false, null, CLOCK))
                .isInstanceOf(TicketNotMutableException.class);
        assertThatThrownBy(() -> ticket.addComment("a", "b", CLOCK))
                .isInstanceOf(TicketNotMutableException.class);
    }

    private static Ticket ticketIn(TicketStatus status) {
        Ticket ticket = newOpenTicket();
        if (status == TicketStatus.OPEN) {
            return ticket;
        }
        if (status == TicketStatus.IN_PROGRESS || status == TicketStatus.CANCELLED) {
            ticket.transitionTo(status, CLOCK);
            return ticket;
        }
        ticket.transitionTo(TicketStatus.IN_PROGRESS, CLOCK);
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            ticket.transitionTo(TicketStatus.RESOLVED, CLOCK);
        }
        if (status == TicketStatus.CLOSED) {
            ticket.transitionTo(TicketStatus.CLOSED, CLOCK);
        }
        return ticket;
    }
}
