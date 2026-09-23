package com.supportticket.ticket.web;

import com.jayway.jsonpath.JsonPath;
import com.supportticket.ticket.domain.TicketStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@AutoConfigureTestDatabase
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void should_create_transition_and_comment_through_api() throws Exception {
        String id = createTicket("VPN disconnects");

        mvc.perform(patch("/api/v1/tickets/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "IN_PROGRESS" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mvc.perform(post("/api/v1/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "authorId": "agent-42", "body": "Checking firewall rules." }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/tickets/" + id + "/comments/")))
                .andExpect(jsonPath("$.body").value("Checking firewall rules."))
                .andExpect(jsonPath("$.ticketId").value(id));

        mvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].authorId").value("agent-42"));
    }

    @ParameterizedTest
    @CsvSource({
            "CLOSED, OPEN",
            "RESOLVED, OPEN",
            "CANCELLED, OPEN"
    })
    void should_return422_problem_when_transition_is_illegal(TicketStatus from, TicketStatus to) throws Exception {
        String id = createTicket("Illegal reopen " + from + " " + to);
        driveTo(id, from);

        mvc.perform(patch("/api/v1/tickets/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "%s" }
                                """.formatted(to)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.support-tickets.local/problems/illegal-ticket-state"))
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(containsString(from.name())))
                .andExpect(jsonPath("$.from").value(from.name()))
                .andExpect(jsonPath("$.to").value(to.name()))
                .andExpect(jsonPath("$.instance").value("/api/v1/tickets/" + id + "/status"));

        mvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(from.name()));
    }

    @Test
    void should_return400_when_create_payload_is_invalid() throws Exception {
        mvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "missing title",
                                  "priority": "HIGH",
                                  "reporterId": "user-9"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.support-tickets.local/problems/validation"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field == 'title')]").exists());
    }

    private String createTicket(String title) throws Exception {
        MvcResult created = mvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Integration test ticket.",
                                  "priority": "HIGH",
                                  "reporterId": "user-9"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    private void driveTo(String id, TicketStatus target) throws Exception {
        switch (target) {
            case OPEN -> {
            }
            case IN_PROGRESS -> patchStatus(id, TicketStatus.IN_PROGRESS);
            case RESOLVED -> {
                patchStatus(id, TicketStatus.IN_PROGRESS);
                patchStatus(id, TicketStatus.RESOLVED);
            }
            case CLOSED -> {
                patchStatus(id, TicketStatus.IN_PROGRESS);
                patchStatus(id, TicketStatus.RESOLVED);
                patchStatus(id, TicketStatus.CLOSED);
            }
            case CANCELLED -> patchStatus(id, TicketStatus.CANCELLED);
        }
    }

    private void patchStatus(String id, TicketStatus status) throws Exception {
        mvc.perform(patch("/api/v1/tickets/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "status": "%s" }
                                """.formatted(status)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status.name()));
    }
}
