package com.supportticket.ticket.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.supportticket.ticket.domain.Priority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(min = 1, max = 200) String title,
        @Size(min = 1, max = 10_000) String description,
        Priority priority,
        @Size(min = 1, max = 128) String assigneeId,
        @JsonIgnore boolean assigneeIdPresent
) {
    @JsonCreator
    public static UpdateTicketRequest fromJson(
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("priority") Priority priority,
            @JsonProperty("assigneeId") JsonNode assigneeIdNode
    ) {
        boolean assigneeIdPresent = assigneeIdNode != null;
        String assigneeId = (assigneeIdNode == null || assigneeIdNode.isNull()) ? null : assigneeIdNode.asText();
        return new UpdateTicketRequest(title, description, priority, assigneeId, assigneeIdPresent);
    }

    @AssertTrue(message = "at least one of title, description, priority, assigneeId is required")
    public boolean hasMutableField() {
        return title != null || description != null || priority != null || assigneeIdPresent;
    }

    public boolean isEmpty() {
        return !hasMutableField();
    }
}
