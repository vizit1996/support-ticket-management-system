package com.supportticket.ticket.api;

import com.supportticket.ticket.domain.Priority;
import com.supportticket.ticket.domain.TicketStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketSummary(
        UUID id,
        String title,
        Priority priority,
        TicketStatus status,
        String assigneeId,
        String reporterId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
