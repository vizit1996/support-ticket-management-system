package com.supportticket.ticket.api;

import com.supportticket.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull TicketStatus status) {
}
