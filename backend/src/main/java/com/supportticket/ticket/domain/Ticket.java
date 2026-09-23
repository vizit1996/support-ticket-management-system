package com.supportticket.ticket.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "tickets",
        indexes = {
                @Index(name = "idx_tickets_status", columnList = "status"),
                @Index(name = "idx_tickets_created_at", columnList = "created_at"),
                @Index(name = "idx_tickets_assignee_id", columnList = "assignee_id"),
                @Index(name = "idx_tickets_status_created_at", columnList = "status, created_at")
        }
)
public class Ticket {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Priority priority;

    @Column(name = "assignee_id", length = 128)
    private String assigneeId;

    @Column(name = "reporter_id", nullable = false, length = 128)
    private String reporterId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.PERSIST, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    protected Ticket() {
        // JPA
    }

    private Ticket(
            UUID id,
            String title,
            String description,
            Priority priority,
            String reporterId,
            String assigneeId,
            OffsetDateTime now
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = TicketStatus.OPEN;
        this.priority = priority;
        this.reporterId = reporterId;
        this.assigneeId = assigneeId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Ticket open(
            String title,
            String description,
            Priority priority,
            String reporterId,
            String assigneeId
    ) {
        return open(title, description, priority, reporterId, assigneeId, Clock.systemUTC());
    }

    public static Ticket open(
            String title,
            String description,
            Priority priority,
            String reporterId,
            String assigneeId,
            Clock clock
    ) {
        return new Ticket(
                UUID.randomUUID(),
                title,
                description,
                Objects.requireNonNull(priority, "priority"),
                Objects.requireNonNull(reporterId, "reporterId"),
                assigneeId,
                OffsetDateTime.now(clock)
        );
    }

    /**
     * Applies a legal status change and timestamp side effects from {@code spec/state-machine.md}.
     */
    public void transitionTo(TicketStatus target) {
        transitionTo(target, Clock.systemUTC());
    }

    public void transitionTo(TicketStatus target, Clock clock) {
        Objects.requireNonNull(target, "target");
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(status, target);
        }
        this.status = target;
        OffsetDateTime now = OffsetDateTime.now(clock);
        this.updatedAt = now;
        switch (target) {
            case RESOLVED -> {
                if (this.resolvedAt == null) {
                    this.resolvedAt = now;
                }
            }
            case CLOSED, CANCELLED -> {
                if (this.closedAt == null) {
                    this.closedAt = now;
                }
            }
            default -> {
                // OPEN / IN_PROGRESS: timestamps already handled via updatedAt
            }
        }
    }

    public void applyFieldUpdate(
            String title,
            String description,
            Priority priority,
            boolean assigneeIdPresent,
            String assigneeId,
            Clock clock
    ) {
        requireMutable();
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (priority != null) {
            this.priority = priority;
        }
        if (assigneeIdPresent) {
            this.assigneeId = assigneeId;
        }
        this.updatedAt = OffsetDateTime.now(clock);
    }

    public Comment addComment(String authorId, String body, Clock clock) {
        requireMutable();
        Comment comment = Comment.append(this, authorId, body, clock);
        this.updatedAt = OffsetDateTime.now(clock);
        return comment;
    }

    private void requireMutable() {
        if (!status.allowsMutation()) {
            throw new TicketNotMutableException(id, status);
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public String getReporterId() {
        return reporterId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<Comment> getComments() {
        return Collections.unmodifiableList(comments);
    }

    void addComment(Comment comment) {
        comments.add(comment);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ticket ticket)) {
            return false;
        }
        return id != null && id.equals(ticket.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
