package com.supportticket.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(
        @NotBlank @Size(min = 1, max = 128) String authorId,
        @NotBlank @Size(min = 1, max = 5_000) String body
) {
}
