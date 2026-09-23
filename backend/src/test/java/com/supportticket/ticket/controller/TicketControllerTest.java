package com.supportticket.ticket.controller;

import com.supportticket.ticket.dto.CommentResponse;
import com.supportticket.ticket.dto.PageTicketSummary;
import com.supportticket.ticket.dto.TicketResponse;
import com.supportticket.ticket.dto.TicketSummary;
import com.supportticket.ticket.domain.Priority;
import com.supportticket.ticket.domain.ResourceNotFoundException;
import com.supportticket.ticket.domain.TicketStatus;
import com.supportticket.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    private static final UUID TICKET_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-22T18:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void should_return201_when_ticketCreated() throws Exception {
        when(ticketService.createTicket(any())).thenReturn(detail(TicketStatus.OPEN, List.of()));

        mvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cannot reset password",
                                  "description": "Reset email never arrives.",
                                  "priority": "HIGH",
                                  "reporterId": "user-9"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tickets/" + TICKET_ID))
                .andExpect(jsonPath("$.id").value(TICKET_ID.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void should_return400_when_create_validation_fails() throws Exception {
        mvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "", "description": "x", "priority": "HIGH", "reporterId": "u" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.support-tickets.local/problems/validation"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void should_list_tickets_with_keyword_alias_and_status() throws Exception {
        var summary = new TicketSummary(
                TICKET_ID, "Cannot reset password", Priority.HIGH, TicketStatus.OPEN,
                null, "user-9", NOW, NOW
        );
        when(ticketService.listTickets(eq("password"), eq(TicketStatus.OPEN), isNull(), isNull()))
                .thenReturn(new PageTicketSummary(List.of(summary), 0, 20, 1, 1));

        mvc.perform(get("/api/v1/tickets").param("keyword", "password").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(TICKET_ID.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(get("/api/v1/tickets").param("q", "password").param("status", "OPEN"))
                .andExpect(status().isOk());
        verify(ticketService, org.mockito.Mockito.times(2))
                .listTickets(eq("password"), eq(TicketStatus.OPEN), isNull(), isNull());
    }

    @Test
    void should_return200_when_get_by_id() throws Exception {
        when(ticketService.getTicketById(TICKET_ID)).thenReturn(detail(TicketStatus.OPEN, List.of()));
        mvc.perform(get("/api/v1/tickets/" + TICKET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Reset email never arrives."));
    }

    @Test
    void should_return404_when_ticket_missing() throws Exception {
        when(ticketService.getTicketById(TICKET_ID)).thenThrow(ResourceNotFoundException.ticket(TICKET_ID));
        mvc.perform(get("/api/v1/tickets/" + TICKET_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.support-tickets.local/problems/ticket-not-found"));
    }

    @Test
    void should_patch_fields_and_status() throws Exception {
        when(ticketService.updateTicket(eq(TICKET_ID), any())).thenReturn(detail(TicketStatus.OPEN, List.of()));
        when(ticketService.updateStatus(TICKET_ID, TicketStatus.IN_PROGRESS))
                .thenReturn(detail(TicketStatus.IN_PROGRESS, List.of()));

        mvc.perform(patch("/api/v1/tickets/" + TICKET_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Password reset emails", "assigneeId": "agent-42" }
                                """))
                .andExpect(status().isOk());

        mvc.perform(patch("/api/v1/tickets/" + TICKET_ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "IN_PROGRESS" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void should_return201_when_comment_added() throws Exception {
        UUID commentId = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
        when(ticketService.addComment(eq(TICKET_ID), any())).thenReturn(
                new CommentResponse(commentId, TICKET_ID, "agent-42", "Asked the user to check spam.", NOW)
        );

        mvc.perform(post("/api/v1/tickets/" + TICKET_ID + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "authorId": "agent-42", "body": "Asked the user to check spam." }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tickets/" + TICKET_ID + "/comments/" + commentId))
                .andExpect(jsonPath("$.body").value("Asked the user to check spam."));
    }

    private static TicketResponse detail(TicketStatus status, List<CommentResponse> comments) {
        return new TicketResponse(
                TICKET_ID,
                "Cannot reset password",
                "Reset email never arrives.",
                Priority.HIGH,
                status,
                null,
                "user-9",
                NOW,
                NOW,
                null,
                null,
                comments
        );
    }
}
