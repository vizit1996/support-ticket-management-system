package com.supportticket.ticket.api;

import com.supportticket.ticket.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        @NotBlank @Size(min = 1, max = 10_000) String description,
        @NotNull Priority priority,
        @Size(min = 1, max = 128) String assigneeId,
        @NotBlank @Size(min = 1, max = 128) String reporterId
) {
}
