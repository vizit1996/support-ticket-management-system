package com.supportticket.ticket.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comments_ticket_id_created_at", columnList = "ticket_id, created_at")
        }
)
public class Comment {

    @Id
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ticket_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_comments_ticket")
    )
    private Ticket ticket;

    @Column(name = "author_id", nullable = false, length = 128)
    private String authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Comment() {
        // JPA
    }

    private Comment(UUID id, Ticket ticket, String authorId, String body, OffsetDateTime createdAt) {
        this.id = id;
        this.ticket = ticket;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = createdAt;
    }

    public static Comment append(Ticket ticket, String authorId, String body) {
        return append(ticket, authorId, body, Clock.systemUTC());
    }

    public static Comment append(Ticket ticket, String authorId, String body, Clock clock) {
        Comment comment = new Comment(
                UUID.randomUUID(),
                Objects.requireNonNull(ticket, "ticket"),
                Objects.requireNonNull(authorId, "authorId"),
                Objects.requireNonNull(body, "body"),
                OffsetDateTime.now(clock)
        );
        ticket.addComment(comment);
        return comment;
    }

    public UUID getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Comment comment)) {
            return false;
        }
        return id != null && id.equals(comment.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
