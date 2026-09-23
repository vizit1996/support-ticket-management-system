package com.supportticket.ticket.service;

import com.supportticket.ticket.api.AddCommentRequest;
import com.supportticket.ticket.api.CreateTicketRequest;
import com.supportticket.ticket.api.PageTicketSummary;
import com.supportticket.ticket.api.UpdateTicketRequest;
import com.supportticket.ticket.domain.InvalidRequestException;
import com.supportticket.ticket.domain.InvalidStateTransitionException;
import com.supportticket.ticket.domain.Priority;
import com.supportticket.ticket.domain.ResourceNotFoundException;
import com.supportticket.ticket.domain.Ticket;
import com.supportticket.ticket.domain.TicketNotMutableException;
import com.supportticket.ticket.domain.TicketStatus;
import com.supportticket.ticket.repository.CommentRepository;
import com.supportticket.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-22T18:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TicketRepository tickets;

    @Mock
    private CommentRepository comments;

    private TicketService service;

    @BeforeEach
    void setUp() {
        service = new TicketService(tickets, comments, CLOCK);
        lenient().when(tickets.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void should_createTicket_in_open_status() {
        var created = service.createTicket(new CreateTicketRequest(
                " Title ",
                "Desc",
                Priority.HIGH,
                null,
                "reporter-1"
        ));
        assertThat(created.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.title()).isEqualTo("Title");
        assertThat(created.comments()).isEmpty();
        verify(tickets).save(any(Ticket.class));
    }

    @Test
    void should_throw_not_found_when_getTicketById_unknown() {
        UUID id = UUID.randomUUID();
        when(tickets.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getTicketById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void should_listTickets_with_search_and_filter() {
        Ticket ticket = Ticket.open("Pwd reset", "email", Priority.LOW, "r", null, CLOCK);
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(tickets.search(eq("pwd"), eq(TicketStatus.OPEN), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(ticket), pageable, 1));

        PageTicketSummary page = service.listTickets(" pwd ", TicketStatus.OPEN, null, null);
        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void should_reject_invalid_list_query_params() {
        assertThatThrownBy(() -> service.listTickets("x", null, -1, 20))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.listTickets("x", null, 0, 101))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.listTickets("   ", null, 0, 20))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void should_updateTicket_fields() {
        Ticket ticket = Ticket.open("Old", "Desc", Priority.LOW, "r", null, CLOCK);
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(comments.findByTicket_IdOrderByCreatedAtAsc(ticket.getId())).thenReturn(List.of());

        var updated = service.updateTicket(
                ticket.getId(),
                new UpdateTicketRequest("New", null, Priority.URGENT, "agent-1", true)
        );
        assertThat(updated.title()).isEqualTo("New");
        assertThat(updated.priority()).isEqualTo(Priority.URGENT);
        assertThat(updated.assigneeId()).isEqualTo("agent-1");
    }

    @Test
    void should_reject_empty_update_and_terminal_mutation() {
        assertThatThrownBy(() -> service.updateTicket(UUID.randomUUID(), new UpdateTicketRequest(null, null, null, null, false)))
                .isInstanceOf(InvalidRequestException.class);

        Ticket ticket = Ticket.open("T", "D", Priority.LOW, "r", null, CLOCK);
        ticket.transitionTo(TicketStatus.CANCELLED, CLOCK);
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateTicket(
                ticket.getId(),
                new UpdateTicketRequest("X", null, null, null, false)
        )).isInstanceOf(TicketNotMutableException.class);
    }

    @Test
    void should_updateStatus_or_throw_illegal_transition() {
        Ticket ticket = Ticket.open("T", "D", Priority.LOW, "r", null, CLOCK);
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(comments.findByTicket_IdOrderByCreatedAtAsc(ticket.getId())).thenReturn(List.of());

        assertThat(service.updateStatus(ticket.getId(), TicketStatus.IN_PROGRESS).status())
                .isEqualTo(TicketStatus.IN_PROGRESS);
        assertThatThrownBy(() -> service.updateStatus(ticket.getId(), TicketStatus.OPEN))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void should_addComment() {
        Ticket ticket = Ticket.open("T", "D", Priority.LOW, "r", null, CLOCK);
        when(tickets.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(comments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var comment = service.addComment(ticket.getId(), new AddCommentRequest("agent", "hello"));
        assertThat(comment.body()).isEqualTo("hello");
        assertThat(comment.ticketId()).isEqualTo(ticket.getId());
        verify(comments).save(any());
    }
}
