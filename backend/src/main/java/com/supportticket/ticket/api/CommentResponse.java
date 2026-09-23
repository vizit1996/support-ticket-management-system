package com.supportticket.ticket.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID ticketId,
        String authorId,
        String body,
        OffsetDateTime createdAt
) {
}
