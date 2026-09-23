package com.supportticket.ticket.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStatusTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-22T18:00:00Z"), ZoneOffset.UTC);

    private static final Set<String> ALLOWED = Set.of(
            "OPEN->IN_PROGRESS",
            "IN_PROGRESS->RESOLVED",
            "RESOLVED->CLOSED",
            "OPEN->CANCELLED",
            "IN_PROGRESS->CANCELLED"
    );

    @ParameterizedTest
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED",
            "OPEN, CANCELLED",
            "IN_PROGRESS, CANCELLED"
    })
    void should_allow_every_legal_transition(TicketStatus from, TicketStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();

        Ticket ticket = ticketIn(from);
        ticket.transitionTo(to, CLOCK);
        assertThat(ticket.getStatus()).isEqualTo(to);
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, OPEN",
            "OPEN, RESOLVED",
            "OPEN, CLOSED",
            "IN_PROGRESS, OPEN",
            "IN_PROGRESS, IN_PROGRESS",
            "IN_PROGRESS, CLOSED",
            "RESOLVED, OPEN",
            "RESOLVED, IN_PROGRESS",
            "RESOLVED, RESOLVED",
            "RESOLVED, CANCELLED",
            "CLOSED, OPEN",
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED",
            "CLOSED, CLOSED",
            "CLOSED, CANCELLED",
            "CANCELLED, OPEN",
            "CANCELLED, IN_PROGRESS",
            "CANCELLED, RESOLVED",
            "CANCELLED, CLOSED",
            "CANCELLED, CANCELLED"
    })
    void should_throw_InvalidStateTransitionException_for_every_illegal_transition(
            TicketStatus from,
            TicketStatus to
    ) {
        assertThat(from.canTransitionTo(to)).isFalse();

        Ticket ticket = ticketIn(from);
        assertThatThrownBy(() -> ticket.transitionTo(to, CLOCK))
                .isInstanceOf(InvalidStateTransitionException.class)
                .satisfies(ex -> {
                    var illegal = (InvalidStateTransitionException) ex;
                    assertThat(illegal.from()).isEqualTo(from);
                    assertThat(illegal.to()).isEqualTo(to);
                });
        assertThat(ticket.getStatus()).isEqualTo(from);
    }

    @ParameterizedTest
    @MethodSource("allPairs")
    void should_match_spec_matrix_for_every_pair(TicketStatus from, TicketStatus to) {
        boolean expected = ALLOWED.contains(from.name() + "->" + to.name());
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    static Stream<Arguments> allPairs() {
        return Arrays.stream(TicketStatus.values())
                .flatMap(from -> Arrays.stream(TicketStatus.values()).map(to -> Arguments.of(from, to)));
    }

    private static Ticket ticketIn(TicketStatus status) {
        Ticket ticket = Ticket.open("Title", "Description", Priority.HIGH, "reporter-1", null, CLOCK);
        switch (status) {
            case OPEN -> {
                return ticket;
            }
            case IN_PROGRESS, CANCELLED -> ticket.transitionTo(status, CLOCK);
            case RESOLVED -> {
                ticket.transitionTo(TicketStatus.IN_PROGRESS, CLOCK);
                ticket.transitionTo(TicketStatus.RESOLVED, CLOCK);
            }
            case CLOSED -> {
                ticket.transitionTo(TicketStatus.IN_PROGRESS, CLOCK);
                ticket.transitionTo(TicketStatus.RESOLVED, CLOCK);
                ticket.transitionTo(TicketStatus.CLOSED, CLOCK);
            }
        }
        return ticket;
    }
}
