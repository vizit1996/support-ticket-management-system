package com.supportticket.ticket.web;

import com.supportticket.ticket.domain.InvalidStateTransitionException;
import com.supportticket.ticket.domain.ResourceNotFoundException;
import com.supportticket.ticket.domain.TicketStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void stubRequest() {
        when(request.getRequestURI()).thenReturn("/api/v1/tickets/550e8400-e29b-41d4-a716-446655440000/status");
    }

    @Test
    void should_map_illegal_transition_to_422_problem() {
        var ex = new InvalidStateTransitionException(TicketStatus.CLOSED, TicketStatus.OPEN);
        var response = handler.handleIllegalTransition(ex, request);
        ProblemDetail body = response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(body.getType().toString()).endsWith("/illegal-ticket-state");
        assertThat(body.getStatus()).isEqualTo(422);
        assertThat(body.getProperties()).containsEntry("from", "CLOSED").containsEntry("to", "OPEN");
    }

    @Test
    void should_map_not_found_to_404_problem() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        var response = handler.handleNotFound(ResourceNotFoundException.ticket(id), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getType().toString()).endsWith("/ticket-not-found");
        assertThat(response.getBody().getDetail()).contains(id.toString());
    }

    @Test
    void should_map_validation_to_400_with_field_errors() throws Exception {
        var target = new Object();
        var binding = new BeanPropertyBindingResult(target, "createTicketRequest");
        binding.addError(new FieldError("createTicketRequest", "title", "must not be blank"));
        MethodParameter parameter = new MethodParameter(String.class.getMethod("toString"), -1);
        var ex = new MethodArgumentNotValidException(parameter, binding);

        var response = handler.handleMethodArgumentNotValid(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getType().toString()).endsWith("/validation");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> errors = (List<Map<String, String>>) response.getBody().getProperties().get("errors");
        assertThat(errors).contains(Map.of("field", "title", "message", "must not be blank"));
    }
}
