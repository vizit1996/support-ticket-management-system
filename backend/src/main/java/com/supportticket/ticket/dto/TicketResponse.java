package com.supportticket.ticket.dto;

import com.supportticket.ticket.domain.Priority;
import com.supportticket.ticket.domain.TicketStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        Priority priority,
        TicketStatus status,
        String assigneeId,
        String reporterId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime resolvedAt,
        OffsetDateTime closedAt,
        List<CommentResponse> comments
) {
}
