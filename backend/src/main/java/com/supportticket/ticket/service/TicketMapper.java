package com.supportticket.ticket.service;

import com.supportticket.ticket.dto.CommentResponse;
import com.supportticket.ticket.dto.PageTicketSummary;
import com.supportticket.ticket.dto.TicketResponse;
import com.supportticket.ticket.dto.TicketSummary;
import com.supportticket.ticket.domain.Comment;
import com.supportticket.ticket.domain.Ticket;
import org.springframework.data.domain.Page;

import java.util.List;

final class TicketMapper {

    private TicketMapper() {
    }

    static TicketSummary toSummary(Ticket ticket) {
        return new TicketSummary(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssigneeId(),
                ticket.getReporterId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    static TicketResponse toDetail(Ticket ticket, List<Comment> comments) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssigneeId(),
                ticket.getReporterId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                comments.stream().map(TicketMapper::toComment).toList()
        );
    }

    static CommentResponse toComment(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthorId(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }

    static PageTicketSummary toPage(Page<Ticket> page) {
        return new PageTicketSummary(
                page.getContent().stream().map(TicketMapper::toSummary).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
