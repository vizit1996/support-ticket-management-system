package com.supportticket.ticket.repository;

import com.supportticket.ticket.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByTicket_IdOrderByCreatedAtAsc(UUID ticketId);
}
