package com.supportticket.ticket.repository;

import com.supportticket.ticket.domain.Comment;
import com.supportticket.ticket.domain.Priority;
import com.supportticket.ticket.domain.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommentRepositoryTest {

    private static final Clock T0 = Clock.fixed(Instant.parse("2026-09-22T18:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private CommentRepository comments;

    @Test
    void should_return_comments_for_ticket_oldest_first() {
        Ticket ticket = tickets.save(Ticket.open("Title", "Desc", Priority.LOW, "user-1", null, T0));
        Ticket other = tickets.save(Ticket.open("Other", "Desc", Priority.LOW, "user-2", null, T0));

        Comment second = comments.save(Comment.append(ticket, "a", "later", Clock.offset(T0, Duration.ofMinutes(2))));
        Comment first = comments.save(Comment.append(ticket, "a", "earlier", Clock.offset(T0, Duration.ofMinutes(1))));
        comments.save(Comment.append(other, "a", "other-ticket", T0));
        tickets.save(ticket);
        tickets.save(other);

        List<Comment> result = comments.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        assertThat(result).extracting(Comment::getId).containsExactly(first.getId(), second.getId());
    }

    @Test
    void should_return_empty_list_when_ticket_has_no_comments() {
        assertThat(comments.findByTicket_IdOrderByCreatedAtAsc(UUID.randomUUID())).isEmpty();
    }
}
