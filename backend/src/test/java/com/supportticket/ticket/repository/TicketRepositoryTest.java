package com.supportticket.ticket.repository;

import com.supportticket.ticket.domain.Comment;
import com.supportticket.ticket.domain.Priority;
import com.supportticket.ticket.domain.Ticket;
import com.supportticket.ticket.domain.TicketStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TicketRepositoryTest {

    private static final Clock T0 = Clock.fixed(Instant.parse("2026-09-22T18:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private CommentRepository comments;

    private Ticket passwordReset;
    private Ticket billing;
    private Ticket vpn;

    @BeforeEach
    void seed() {
        passwordReset = tickets.save(Ticket.open(
                "Cannot reset password",
                "Reset email never arrives.",
                Priority.HIGH,
                "user-1",
                null,
                T0
        ));
        billing = tickets.save(Ticket.open(
                "Invoice mismatch",
                "Charged twice for September.",
                Priority.MEDIUM,
                "user-2",
                "agent-1",
                Clock.offset(T0, Duration.ofMinutes(1))
        ));
        vpn = tickets.save(Ticket.open(
                "VPN timeout",
                "Client disconnects after 30 seconds.",
                Priority.LOW,
                "user-3",
                null,
                Clock.offset(T0, Duration.ofMinutes(2))
        ));
        vpn.transitionTo(TicketStatus.IN_PROGRESS, Clock.offset(T0, Duration.ofMinutes(3)));
        tickets.save(vpn);
    }

    @Test
    void should_return_all_tickets_newest_first_when_unfiltered() {
        Page<Ticket> page = tickets.search(null, null, newestFirst());
        assertThat(page.getContent()).extracting(Ticket::getTitle)
                .containsExactly("VPN timeout", "Invoice mismatch", "Cannot reset password");
    }

    @Test
    void should_filter_by_status() {
        Page<Ticket> page = tickets.search(null, TicketStatus.IN_PROGRESS, newestFirst());
        assertThat(page.getContent()).extracting(Ticket::getId).containsExactly(vpn.getId());
    }

    @Test
    void should_search_title_or_description_case_insensitive() {
        Page<Ticket> byTitle = tickets.search("PASSWORD", null, newestFirst());
        assertThat(byTitle.getContent()).extracting(Ticket::getId).containsExactly(passwordReset.getId());

        Page<Ticket> byDescription = tickets.search("september", null, newestFirst());
        assertThat(byDescription.getContent()).extracting(Ticket::getId).containsExactly(billing.getId());
    }

    @Test
    void should_combine_keyword_and_status_with_and() {
        Page<Ticket> matches = tickets.search("vpn", TicketStatus.IN_PROGRESS, newestFirst());
        assertThat(matches.getContent()).extracting(Ticket::getId).containsExactly(vpn.getId());

        Page<Ticket> noMatch = tickets.search("vpn", TicketStatus.OPEN, newestFirst());
        assertThat(noMatch.getContent()).isEmpty();
    }

    @Test
    void should_not_search_comment_bodies() {
        comments.save(Comment.append(billing, "agent-1", "unique-comment-token", T0));
        tickets.save(billing);

        Page<Ticket> page = tickets.search("unique-comment-token", null, newestFirst());
        assertThat(page.getContent()).isEmpty();
    }

    private static PageRequest newestFirst() {
        return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
