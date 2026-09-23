# REST API Contract — `/api/v1`

Wire contract for the Support Ticket Management System. Field names are **camelCase**. Java records at the API boundary must match these shapes. Functional meaning is in `spec/requirements.md`; transitions in `spec/state-machine.md`.

**Base path:** `/api/v1`

| Content | Media type |
| --- | --- |
| Request / success | `application/json` |
| Errors | `application/problem+json` (RFC 7807) |

Path parameters named `{id}` are ticket UUIDs unless noted.

---

## Enums

**`TicketStatus`:** `OPEN` | `IN_PROGRESS` | `RESOLVED` | `CLOSED` | `CANCELLED`

**`Priority`:** `LOW` | `MEDIUM` | `HIGH` | `URGENT`

Timestamps are ISO-8601 UTC with offset, e.g. `2026-09-22T18:00:00Z`.

---

## Resource schemas

### `TicketSummary`

List page item. No `description`, no `comments`.

| Field | Type | Nullable | Notes |
| --- | --- | --- | --- |
| `id` | UUID string | no | |
| `title` | string | no | |
| `priority` | `Priority` | no | |
| `status` | `TicketStatus` | no | |
| `assigneeId` | string | **yes** | |
| `reporterId` | string | no | |
| `createdAt` | datetime | no | |
| `updatedAt` | datetime | no | |

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Cannot reset password",
  "priority": "HIGH",
  "status": "OPEN",
  "assigneeId": "agent-42",
  "reporterId": "user-9",
  "createdAt": "2026-09-22T18:00:00Z",
  "updatedAt": "2026-09-22T18:00:00Z"
}
```

### `TicketDetail`

Returned from create, get-by-id, field update, and status change.

| Field | Type | Nullable | Notes |
| --- | --- | --- | --- |
| `id` | UUID string | no | |
| `title` | string | no | 1–200 |
| `description` | string | no | 1–10_000 |
| `priority` | `Priority` | no | |
| `status` | `TicketStatus` | no | |
| `assigneeId` | string | **yes** | |
| `reporterId` | string | no | |
| `createdAt` | datetime | no | |
| `updatedAt` | datetime | no | |
| `resolvedAt` | datetime | yes | |
| `closedAt` | datetime | yes | |
| `comments` | `Comment[]` | no | `createdAt` ascending; `[]` if none |

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "priority": "HIGH",
  "status": "OPEN",
  "assigneeId": null,
  "reporterId": "user-9",
  "createdAt": "2026-09-22T18:00:00Z",
  "updatedAt": "2026-09-22T18:00:00Z",
  "resolvedAt": null,
  "closedAt": null,
  "comments": []
}
```

### `Comment`

| Field | Type | Nullable |
| --- | --- | --- |
| `id` | UUID string | no |
| `ticketId` | UUID string | no |
| `authorId` | string | no |
| `body` | string | no |
| `createdAt` | datetime | no |

```json
{
  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "ticketId": "550e8400-e29b-41d4-a716-446655440000",
  "authorId": "agent-42",
  "body": "Asked the user to check spam.",
  "createdAt": "2026-09-22T18:05:00Z"
}
```

### `PageTicketSummary`

| Field | Type | Notes |
| --- | --- | --- |
| `content` | `TicketSummary[]` | |
| `page` | int | 0-based |
| `size` | int | page size requested |
| `totalElements` | long | |
| `totalPages` | int | |

Do not leak Spring `PageImpl` fields (`pageable`, `sort`, `first`, `last`) unless this schema is revised.

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

### Request: `CreateTicketRequest`

| Field | Required | Rules |
| --- | --- | --- |
| `title` | yes | 1–200, trimmed, not blank |
| `description` | yes | 1–10_000, not blank |
| `priority` | yes | `Priority` |
| `assigneeId` | no | 1–128 if present; JSON `null` or omit = unassigned |
| `reporterId` | yes | 1–128 |

```json
{
  "title": "Cannot reset password",
  "description": "Reset email never arrives.",
  "priority": "HIGH",
  "assigneeId": null,
  "reporterId": "user-9"
}
```

### Request: `UpdateTicketRequest`

All fields optional; **at least one** of `title`, `description`, `priority`, `assigneeId` required. Do **not** include `status`.

| Field | Rules if present |
| --- | --- |
| `title` | same as create |
| `description` | same as create |
| `priority` | `Priority` |
| `assigneeId` | string 1–128, or JSON `null` to unassign |

```json
{
  "title": "Password reset emails",
  "description": "Updated repro steps.",
  "priority": "URGENT",
  "assigneeId": "agent-42"
}
```

### Request: `ChangeStatusRequest`

| Field | Required | Rules |
| --- | --- | --- |
| `status` | yes | `TicketStatus`; must be a **legal** next status |

```json
{
  "status": "IN_PROGRESS"
}
```

### Request: `AddCommentRequest`

| Field | Required | Rules |
| --- | --- | --- |
| `authorId` | yes | 1–128 |
| `body` | yes | 1–5_000, not blank |

```json
{
  "authorId": "agent-42",
  "body": "Asked the user to check spam."
}
```

---

## Endpoints

### Create ticket — FR-1

`POST /api/v1/tickets`

| | |
| --- | --- |
| Body | `CreateTicketRequest` |
| Success | **201** `TicketDetail` |
| Headers | `Location: /api/v1/tickets/{id}` |
| Errors | **400** `validation` |

### List / search / filter — FR-2, FR-6, FR-7

`GET /api/v1/tickets`

| Query | Type | Default | Rules |
| --- | --- | --- | --- |
| `q` | string | omitted | keyword search (FR-6); trimmed; 1–200 chars if present |
| `status` | `TicketStatus` | omitted | exact match (FR-7) |
| `page` | int ≥ 0 | `0` | **400** if negative |
| `size` | int 1–100 | `20` | **400** if out of range |

`q` and `status` may be combined (**AND**). Success: **200** `PageTicketSummary`.

Examples:

- `GET /api/v1/tickets`
- `GET /api/v1/tickets?q=password`
- `GET /api/v1/tickets?status=OPEN`
- `GET /api/v1/tickets?q=password&status=OPEN&page=0&size=20`

### Get ticket details — FR-3

`GET /api/v1/tickets/{id}`

| | |
| --- | --- |
| Path | `id` UUID |
| Success | **200** `TicketDetail` |
| Errors | **400** malformed UUID; **404** `ticket-not-found` |

### Update title / description / priority / assignee — FR-4

`PATCH /api/v1/tickets/{id}`

| | |
| --- | --- |
| Body | `UpdateTicketRequest` |
| Success | **200** `TicketDetail` |
| Errors | **400** validation / empty patch; **404**; **422** `ticket-not-mutable` |

### Change status — FR-4b

`PATCH /api/v1/tickets/{id}/status`

| | |
| --- | --- |
| Body | `ChangeStatusRequest` |
| Success | **200** `TicketDetail` |
| Errors | **400** missing/invalid status; **404**; **422** `illegal-ticket-state` |

### Add comment — FR-5

`POST /api/v1/tickets/{id}/comments`

| | |
| --- | --- |
| Body | `AddCommentRequest` |
| Success | **201** `Comment` |
| Headers | `Location: /api/v1/tickets/{id}/comments/{commentId}` |
| Errors | **400** validation; **404**; **422** `ticket-not-mutable` |

There is no list-comments-only endpoint in v1; comments are embedded on `TicketDetail`.

---

## RFC 7807 error payload

Every error uses Spring `ProblemDetail` with `Content-Type: application/problem+json`. Do not return `{ "message": "..." }` or Spring default `timestamp` / `error` / `path` envelopes.

### Common members

| Member | Type | Required | Notes |
| --- | --- | --- | --- |
| `type` | URI | yes | `https://api.support-tickets.local/problems/{code}` |
| `title` | string | yes | short summary |
| `status` | int | yes | same as HTTP status |
| `detail` | string | yes | human-readable; **no** stack traces |
| `instance` | URI | recommended | request path |

### Validation extensions (**400**)

```json
{
  "type": "https://api.support-tickets.local/problems/validation",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed.",
  "instance": "/api/v1/tickets",
  "errors": [
    { "field": "title", "message": "must not be blank" },
    { "field": "priority", "message": "must be a valid Priority" }
  ]
}
```

`errors` is `{ "field": string, "message": string }[]`. Used for Bean Validation and malformed query params (`page`, `size`, `q`, `status`).

### Problem types

| `type` suffix | HTTP | When |
| --- | --- | --- |
| `validation` | 400 | Bean Validation, empty PATCH, bad query, malformed UUID |
| `invalid-status` | 400 | `status` not a `TicketStatus` |
| `ticket-not-found` | 404 | unknown ticket id |
| `illegal-ticket-state` | 422 | transition not in `spec/state-machine.md` |
| `ticket-not-mutable` | 422 | PATCH fields or POST comment on `CLOSED` / `CANCELLED` |
| `internal` | 500 | unexpected failure (generic `detail` only) |

### Example: illegal transition (**422**)

```json
{
  "type": "https://api.support-tickets.local/problems/illegal-ticket-state",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Transition from CLOSED to OPEN is not allowed.",
  "instance": "/api/v1/tickets/550e8400-e29b-41d4-a716-446655440000/status",
  "from": "CLOSED",
  "to": "OPEN"
}
```

Optional extensions `from` and `to` are allowed on `illegal-ticket-state`.

### Example: not found (**404**)

```json
{
  "type": "https://api.support-tickets.local/problems/ticket-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Ticket 550e8400-e29b-41d4-a716-446655440000 was not found.",
  "instance": "/api/v1/tickets/550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Status code summary

| Code | Use |
| --- | --- |
| 200 | GET, PATCH (fields or status) success |
| 201 | POST create ticket, POST comment |
| 400 | syntax / validation / invalid enum / bad query |
| 404 | ticket does not exist |
| 422 | illegal state transition or mutation of a terminal ticket |
| 500 | unhandled server error |

There is no **409** in v1 (no unique business key besides generated UUID).
---
