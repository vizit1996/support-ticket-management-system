package com.supportticket.ticket.web;

import com.supportticket.ticket.api.AddCommentRequest;
import com.supportticket.ticket.api.CommentResponse;
import com.supportticket.ticket.api.CreateTicketRequest;
import com.supportticket.ticket.api.PageTicketSummary;
import com.supportticket.ticket.api.TicketResponse;
import com.supportticket.ticket.api.UpdateStatusRequest;
import com.supportticket.ticket.api.UpdateTicketRequest;
import com.supportticket.ticket.domain.TicketStatus;
import com.supportticket.ticket.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse created = ticketService.createTicket(request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + created.id())).body(created);
    }

    /**
     * Keyword search uses contract param {@code q} (FR-6). {@code keyword} is accepted as an alias.
     */
    @GetMapping
    public PageTicketSummary list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        String query = q != null ? q : keyword;
        return ticketService.listTickets(query, status, page, size);
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable UUID id) {
        return ticketService.getTicketById(id);
    }

    @PatchMapping("/{id}")
    public TicketResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.updateTicket(id, request);
    }

    @PatchMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return ticketService.updateStatus(id, request.status());
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest request
    ) {
        CommentResponse created = ticketService.addComment(id, request);
        URI location = URI.create("/api/v1/tickets/" + id + "/comments/" + created.id());
        return ResponseEntity.created(location).body(created);
    }
}
