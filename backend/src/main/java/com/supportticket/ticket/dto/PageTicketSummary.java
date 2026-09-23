package com.supportticket.ticket.dto;

import java.util.List;

public record PageTicketSummary(
        List<TicketSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
