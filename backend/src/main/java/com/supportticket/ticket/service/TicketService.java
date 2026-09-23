package com.supportticket.ticket.service;

import com.supportticket.ticket.dto.AddCommentRequest;
import com.supportticket.ticket.dto.CommentResponse;
import com.supportticket.ticket.dto.CreateTicketRequest;
import com.supportticket.ticket.dto.PageTicketSummary;
import com.supportticket.ticket.dto.TicketResponse;
import com.supportticket.ticket.dto.UpdateTicketRequest;
import com.supportticket.ticket.domain.Comment;
import com.supportticket.ticket.domain.InvalidRequestException;
import com.supportticket.ticket.domain.ResourceNotFoundException;
import com.supportticket.ticket.domain.Ticket;
import com.supportticket.ticket.domain.TicketStatus;
import com.supportticket.ticket.repository.CommentRepository;
import com.supportticket.ticket.repository.TicketRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    static final int DEFAULT_PAGE = 0;
    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 100;
    static final int MAX_QUERY_LENGTH = 200;

    private final TicketRepository tickets;
    private final CommentRepository comments;
    private final Clock clock;

    public TicketService(TicketRepository tickets, CommentRepository comments, Clock clock) {
        this.tickets = tickets;
        this.comments = comments;
        this.clock = clock;
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = Ticket.open(
                request.title().trim(),
                request.description(),
                request.priority(),
                request.reporterId(),
                request.assigneeId(),
                clock
        );
        return TicketMapper.toDetail(tickets.save(ticket), List.of());
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(UUID id) {
        Ticket ticket = requireTicket(id);
        return TicketMapper.toDetail(ticket, comments.findByTicket_IdOrderByCreatedAtAsc(id));
    }

    @Transactional(readOnly = true)
    public PageTicketSummary listTickets(String q, TicketStatus status, Integer page, Integer size) {
        int pageNumber = page == null ? DEFAULT_PAGE : page;
        int pageSize = size == null ? DEFAULT_SIZE : size;
        if (pageNumber < 0) {
            throw InvalidRequestException.validation(
                    "Request validation failed.",
                    List.of(new InvalidRequestException.FieldError("page", "must be greater than or equal to 0"))
            );
        }
        if (pageSize < 1 || pageSize > MAX_SIZE) {
            throw InvalidRequestException.validation(
                    "Request validation failed.",
                    List.of(new InvalidRequestException.FieldError("size", "must be between 1 and 100"))
            );
        }
        String keyword = normalizeQuery(q);
        var pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return TicketMapper.toPage(tickets.search(keyword, status, pageable));
    }

    @Transactional
    public TicketResponse updateTicket(UUID id, UpdateTicketRequest command) {
        if (command == null || command.isEmpty()) {
            throw InvalidRequestException.validation(
                    "Request validation failed.",
                    List.of(new InvalidRequestException.FieldError(
                            "body",
                            "at least one of title, description, priority, assigneeId is required"
                    ))
            );
        }
        Ticket ticket = requireTicket(id);
        ticket.applyFieldUpdate(
                command.title(),
                command.description(),
                command.priority(),
                command.assigneeIdPresent(),
                command.assigneeId(),
                clock
        );
        return TicketMapper.toDetail(tickets.save(ticket), comments.findByTicket_IdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public TicketResponse updateStatus(UUID id, TicketStatus target) {
        if (target == null) {
            throw InvalidRequestException.validation(
                    "Request validation failed.",
                    List.of(new InvalidRequestException.FieldError("status", "must not be null"))
            );
        }
        Ticket ticket = requireTicket(id);
        ticket.transitionTo(target, clock);
        return TicketMapper.toDetail(tickets.save(ticket), comments.findByTicket_IdOrderByCreatedAtAsc(id));
    }

    @Transactional
    public CommentResponse addComment(UUID ticketId, AddCommentRequest request) {
        Ticket ticket = requireTicket(ticketId);
        Comment comment = ticket.addComment(request.authorId(), request.body(), clock);
        comments.save(comment);
        tickets.save(ticket);
        return TicketMapper.toComment(comment);
    }

    private Ticket requireTicket(UUID id) {
        return tickets.findById(id).orElseThrow(() -> ResourceNotFoundException.ticket(id));
    }

    private static String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_QUERY_LENGTH) {
            throw InvalidRequestException.validation(
                    "Request validation failed.",
                    List.of(new InvalidRequestException.FieldError("q", "must be between 1 and 200 characters after trim"))
            );
        }
        return trimmed;
    }
}
