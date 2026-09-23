package com.supportticket.ticket.repository;

import com.supportticket.ticket.domain.Ticket;
import com.supportticket.ticket.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    /**
     * Paginated list with optional case-insensitive title/description keyword and optional status.
     * Pass {@code null} or blank keyword and/or {@code null} status to skip that filter.
     */
    default Page<Ticket> search(String keyword, TicketStatus status, Pageable pageable) {
        return findAll(TicketSpecifications.withKeywordAndStatus(keyword, status), pageable);
    }
}
