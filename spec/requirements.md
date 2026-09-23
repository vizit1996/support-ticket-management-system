# Functional Requirements — Support Ticket Management System

This file is the **functional** source of truth for v1. Implementation and generated code **must** match these behaviors. If code and this document disagree, this document wins until it is updated.

| Spec | Role |
| --- | --- |
| `spec/requirements.md` | What the system shall do (this file) |
| `spec/state-machine.md` | Legal and illegal `TicketStatus` transitions |
| `spec/api-contract.md` | HTTP paths, payloads, query params, RFC 7807 |
| `spec/data-model.md` | JPA `Ticket` / `Comment` fields, indexes, relationships |

Non-functional coding constraints (Java 21, Spring Boot layers, tests) live in `.cursor/rules/`. Do not invent features that are not listed here.

**Actors:** any support user of the system. Authentication is **out of scope** for v1 (no login, roles, or ACL). All operations are exposed on HTTP `/api/v1`.

**Identifiers:** tickets and comments use UUID strings. Users (reporter, assignee, comment author) are a non-empty string `userId` (email or opaque id). There is **no User entity** in v1.

**Persistence of records:** tickets and comments are never hard-deleted in v1.

---

## FR-1 Create ticket

The system **shall** create a ticket when the client submits a title, description, priority, and reporter.

| Field | Required | Rules |
| --- | --- | --- |
| `title` | yes | 1–200 characters, trimmed, not blank |
| `description` | yes | 1–10_000 characters, not blank |
| `priority` | yes | `LOW` \| `MEDIUM` \| `HIGH` \| `URGENT` |
| `assigneeId` | no | if present: 1–128 characters; if omitted, `assigneeId` is `null` (unassigned) |
| `reporterId` | yes | 1–128 characters (who opened the ticket) |

**Behavior:**

- Status is always **`OPEN`** on create (see `spec/state-machine.md`).
- `createdAt` and `updatedAt` are set from the server clock (UTC).
- `resolvedAt` and `closedAt` are `null`.
- Comments collection is empty.
- Response: HTTP **201** with `TicketDetail` and `Location` (`spec/api-contract.md`).

**Out of scope:** attachments, watchers, SLA clocks, email ingest, categories/tags.

---

## FR-2 List tickets

The system **shall** return a paginated list of ticket **summaries** (no description body, no comments).

- Default sort: `createdAt` **descending** (newest first).
- Default page: `page=0`, `size=20`. Maximum `size` is **100**. Negative `page` or `size` outside 1–100 → HTTP **400**.
- Empty result: HTTP **200** with `content: []` and `totalElements = 0`.
- List **shall** apply search (FR-6) and status filter (FR-7) when those query parameters are present. Combining both is **AND**.

Schema: `PageTicketSummary` in `spec/api-contract.md`.

---

## FR-3 View ticket details

The system **shall** return one ticket by id, including:

- All ticket fields (title, description, priority, status, assignee, reporter, timestamps).
- Comments ordered by `createdAt` **ascending** (oldest first).

Unknown id → HTTP **404** ProblemDetail (`ticket-not-found`). Malformed UUID → HTTP **400** (`validation`).

---

## FR-4 Update title, description, priority, assignee

The system **shall** allow **partial** updates of mutable ticket fields via `PATCH /api/v1/tickets/{id}`.

| Field | Mutable | Notes |
| --- | --- | --- |
| `title` | yes | same validation as FR-1 |
| `description` | yes | same validation as FR-1 |
| `priority` | yes | same enum as FR-1 |
| `assigneeId` | yes | set to a user id, or JSON `null` to unassign |
| `status` | **no** | status changes are FR-4b only |
| `reporterId` | no | immutable after create |
| `id`, timestamps | no | server-owned |

**Behavior:**

- Omitted fields are unchanged.
- At least one mutable field must be present; otherwise HTTP **400** (`validation`).
- `updatedAt` is refreshed on any successful change.
- Allowed when status is **`OPEN`**, **`IN_PROGRESS`**, or **`RESOLVED`**.
- Rejected when status is **`CLOSED`** or **`CANCELLED`**: HTTP **422**, `ticket-not-mutable` (row unchanged).

---

## FR-4b Change status

The system **shall** change ticket status **only** through `PATCH /api/v1/tickets/{id}/status`, and only along transitions in `spec/state-machine.md`.

- Legal transition → HTTP **200** with updated `TicketDetail`.
- Illegal transition (including self-transition) → HTTP **422**, `illegal-ticket-state` (ticket unchanged).
- Unknown ticket → HTTP **404**.
- Unknown enum value → HTTP **400**, `invalid-status`.

Timestamp side effects:

| Resulting status | Extra field |
| --- | --- |
| `RESOLVED` | `resolvedAt` set if previously null |
| `CLOSED` | `closedAt` set if previously null |
| `CANCELLED` | `closedAt` set if previously null |
| any other | those fields unchanged |

---

## FR-5 Add comments

The system **shall** append a comment to a ticket (`POST /api/v1/tickets/{id}/comments`).

| Field | Required | Rules |
| --- | --- | --- |
| `body` | yes | 1–5_000 characters, not blank |
| `authorId` | yes | 1–128 characters |

**Behavior:**

- Comments are **append-only** in v1 (no edit, no delete, no list-only endpoint).
- Allowed when status is **`OPEN`**, **`IN_PROGRESS`**, or **`RESOLVED`**.
- Rejected when **`CLOSED`** or **`CANCELLED`**: HTTP **422**, `ticket-not-mutable`.
- Success: HTTP **201** `Comment`, `Location` header; parent ticket `updatedAt` refreshed.
- Unknown ticket id → HTTP **404**.

---

## FR-6 Search by keyword

The system **shall** filter the ticket list when query parameter `q` is present.

- Case-insensitive substring match on **`title`** and **`description`**.
- A ticket matches if **either** field contains the keyword.
- Trim leading/trailing whitespace. After trim, `q` must be 1–200 characters; otherwise HTTP **400**.
- Comment bodies are **not** searched in v1.
- Composes with pagination (FR-2) and status filter (FR-7) using AND.

---

## FR-7 Filter by status

The system **shall** filter the ticket list when query parameter `status` is present.

- Single `TicketStatus` value: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`.
- Unknown value → HTTP **400** (`invalid-status`).
- Omitted `status` → tickets of **all** statuses.
- Composes with pagination (FR-2) and keyword search (FR-6) using AND.

---

## FR-8 Non-functional (v1)

- Persistence: PostgreSQL (runtime), H2 (tests / local profile) — `spec/data-model.md`.
- API errors: RFC 7807 only — `spec/api-contract.md`.
- Concurrency: optimistic locking on `Ticket.version` (`spec/data-model.md`).
- No hard delete of tickets or comments.

---

## Acceptance mapping

| Capability | Requirement | Primary API |
| --- | --- | --- |
| Create | FR-1 | `POST /api/v1/tickets` |
| List | FR-2 | `GET /api/v1/tickets` |
| View details | FR-3 | `GET /api/v1/tickets/{id}` |
| Update fields | FR-4 | `PATCH /api/v1/tickets/{id}` |
| Change status | FR-4b | `PATCH /api/v1/tickets/{id}/status` |
| Add comments | FR-5 | `POST /api/v1/tickets/{id}/comments` |
| Search keyword | FR-6 | `GET /api/v1/tickets?q=` |
| Filter status | FR-7 | `GET /api/v1/tickets?status=` |
---
