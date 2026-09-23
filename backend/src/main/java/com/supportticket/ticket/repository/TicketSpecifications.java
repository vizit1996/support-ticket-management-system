package com.supportticket.ticket.repository;

import com.supportticket.ticket.domain.Ticket;
import com.supportticket.ticket.domain.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * List filters for FR-6 (keyword) and FR-7 (status). Blank keyword / null status mean "no filter".
 * Trim and enum validation belong in the API layer, not here.
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }

    public static Specification<Ticket> hasStatus(TicketStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> withKeywordAndStatus(String keyword, TicketStatus status) {
        return Specification.allOf(keywordContains(keyword), hasStatus(status));
    }
}
