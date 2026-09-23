# Data Model — JPA Entities

Source of truth for persistence of **`Ticket`** and **`Comment`**. Runtime database: **PostgreSQL**. Tests / local profile: **H2**. Entity classes live in the domain layer; APIs **never** return entities (map to records in `spec/api-contract.md`).

Naming: Java camelCase ↔ tables/columns **snake_case**. Enums stored as `STRING`. Timestamps are `timestamptz` (UTC) on PostgreSQL and `timestamp` on H2.

There is **no** `User` table in v1. `reporter_id`, `assignee_id`, and `author_id` are unconstrained strings.

Optimistic locking is required on `Ticket` via `@Version long version` (column `version`). Comments are not versioned.

---

## Entity: `Ticket`

| | |
| --- | --- |
| Table | `tickets` |
| Java | `Ticket` |
| Identity | UUID, assigned in application code (`UUID.randomUUID()`) or `@UuidGenerator` before persist |

### Fields

| Java field | Column | JDBC / JPA type | Nullable | Notes |
| --- | --- | --- | --- | --- |
| `id` | `id` | `UUID` / `@Id` | no | PK |
| `title` | `title` | `VARCHAR(200)` | no | FR-1 |
| `description` | `description` | `TEXT` (PostgreSQL) / `VARCHAR(10000)` | no | length still validated at API |
| `priority` | `priority` | `@Enumerated(EnumType.STRING)` `VARCHAR(16)` | no | `Priority` |
| `status` | `status` | `@Enumerated(EnumType.STRING)` `VARCHAR(32)` | no | `TicketStatus`; default `OPEN` |
| `assigneeId` | `assignee_id` | `VARCHAR(128)` | **yes** | unassigned = SQL `NULL` |
| `reporterId` | `reporter_id` | `VARCHAR(128)` | no | immutable after insert |
| `createdAt` | `created_at` | `OffsetDateTime` | no | set on persist |
| `updatedAt` | `updated_at` | `OffsetDateTime` | no | set on persist and every mutation |
| `resolvedAt` | `resolved_at` | `OffsetDateTime` | yes | set on enter `RESOLVED` |
| `closedAt` | `closed_at` | `OffsetDateTime` | yes | set on enter `CLOSED` or `CANCELLED` |
| `version` | `version` | `long` / `@Version` | no | optimistic lock; Hibernate-managed |

Prefer **domain methods** for timestamps and status so state-machine side effects stay in one place. Do not mix `@CreatedDate` / `@LastModifiedDate` with conflicting manual updates.

### Relationships

```text
Ticket 1 ──< Comment
```

- `Ticket.comments`: `@OneToMany(mappedBy = "ticket", cascade = CascadeType.PERSIST, orphanRemoval = false)`, fetch **`LAZY`**.
- List endpoints **must not** load comments (projections / `TicketSummary` queries without `join fetch` comments).
- Detail and update responses may `join fetch` comments or load them in the service after the ticket is loaded.
- `orphanRemoval = false`: comments are append-only; never `clear()` the collection.
- Alternative: persist `Comment` via `CommentRepository` with no cascade from `Ticket`. Pick one style and use it consistently.

### Indexes

| Name | Columns | Purpose |
| --- | --- | --- |
| `pk_tickets` | `id` | primary key |
| `idx_tickets_status` | `status` | FR-7 filter |
| `idx_tickets_created_at` | `created_at DESC` | default list sort |
| `idx_tickets_assignee_id` | `assignee_id` | optional assignee lookup (nullable) |
| `idx_tickets_status_created_at` | `status`, `created_at DESC` | filter + sort |

Keyword search (`q`) in v1: `LOWER(title) LIKE` / `LOWER(description) LIKE` (or `ILIKE` on PostgreSQL). No full-text index in v1.

DDL sketch (PostgreSQL; illustrative — not a migration to execute from this spec):

```sql
CREATE TABLE tickets (
    id              UUID PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    priority        VARCHAR(16) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    assignee_id     VARCHAR(128),
    reporter_id     VARCHAR(128) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    resolved_at     TIMESTAMPTZ,
    closed_at       TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_created_at ON tickets (created_at DESC);
CREATE INDEX idx_tickets_assignee_id ON tickets (assignee_id);
CREATE INDEX idx_tickets_status_created_at ON tickets (status, created_at DESC);
```

---

## Entity: `Comment`

| | |
| --- | --- |
| Table | `comments` |
| Java | `Comment` |
| Identity | UUID PK, same generation strategy as `Ticket` |

### Fields

| Java field | Column | JDBC / JPA type | Nullable | Notes |
| --- | --- | --- | --- | --- |
| `id` | `id` | `UUID` / `@Id` | no | PK |
| `ticket` | `ticket_id` | `@ManyToOne(optional = false, fetch = LAZY)` | no | FK → `tickets.id` |
| `authorId` | `author_id` | `VARCHAR(128)` | no | |
| `body` | `body` | `TEXT` / `VARCHAR(5000)` | no | API max 5_000 |
| `createdAt` | `created_at` | `OffsetDateTime` | no | immutable after insert |

No `updated_at`: comments are not editable in v1.

### Relationships

- Owning side: `Comment.ticket` with `@JoinColumn(name = "ticket_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comments_ticket"))`.
- `ON DELETE`: **`RESTRICT`**. Tickets are not deleted in v1; do not use `ON DELETE CASCADE`.

### Indexes

| Name | Columns | Purpose |
| --- | --- | --- |
| `pk_comments` | `id` | primary key |
| `fk_comments_ticket` | `ticket_id` | FK |
| `idx_comments_ticket_id_created_at` | `ticket_id`, `created_at ASC` | detail view comment order |

```sql
CREATE TABLE comments (
    id          UUID PRIMARY KEY,
    ticket_id   UUID NOT NULL,
    author_id   VARCHAR(128) NOT NULL,
    body        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id)
);

CREATE INDEX idx_comments_ticket_id_created_at
    ON comments (ticket_id, created_at ASC);
```

---

## Mapping rules

- Do not expose `Ticket` / `Comment` from controllers.
- `equals` / `hashCode`: identity on `id` only after persist. Do not include the `comments` collection.
- `TicketRepository extends JpaRepository<Ticket, UUID>` plus queries for `status`, pagination, and keyword (`ContainingIgnoreCase` on title/description, or `@Query`).
- `CommentRepository` for append-only saves if not cascading from `Ticket`.
- H2 must share the same logical schema (UUID, VARCHAR enums). Avoid PostgreSQL-only types in entities without a dialect-safe equivalent (`TEXT` is acceptable).

## Invariants (domain/service, not only DB)

1. `status` is always a defined `TicketStatus`.
2. Transitions follow `spec/state-machine.md`.
3. `resolvedAt` is non-null once the ticket has ever been `RESOLVED` (v1 does not clear it).
4. `closedAt` is non-null for `CLOSED` and `CANCELLED`.
5. A `Comment` always references an existing `Ticket`.
---
