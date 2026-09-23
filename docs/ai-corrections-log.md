# AI Corrections Log

Audit date: 2026-09-23  
Scope: backend source, configuration, tests, frontend integration, steering rules, and `spec/` contracts.

This log records implementation mistakes and anti-patterns introduced during the AI-assisted build. It is an audit trail, not a claim that every item was fixed in this review. Status values distinguish corrected issues from open follow-up work.

## Verification summary

- Java/Spring layering: **mostly compliant**. Controllers delegate to services; repositories contain persistence concerns; status transitions are enforced by the domain.
- State machine: **compliant on the primary path**. All five legal transitions and all twenty illegal pairs are covered by parameterized domain tests.
- API errors: **mostly compliant**, with an invalid-status body mismatch described below.
- Secret scan: **passed for committed values**. No API keys, access tokens, private keys, JWTs, or raw production credentials were found.
- Configuration safety: **needs follow-up**. The runtime database password has an empty fallback and the H2 console is enabled whenever the `h2` profile is active.

## Corrections

### AI-001 — Java toolchain pin contradicts Java 21

**Severity:** Critical  
**Status:** Corrected on 2026-09-23

The original `.sdkmanrc` pinned `java=8.0.432-amzn`, while `backend/pom.xml`, the steering rule, and the README required Java 21. Spring Boot 3.4 also requires Java 17 or newer. An SDKMAN auto-environment would therefore have selected a JDK that could not build the application.

**Evidence**

- `.sdkmanrc` revision history / this audit log
- `backend/pom.xml:21`
- `.cursor/rules/java-springboot.mdc`
- `README.md` toolchain section

**Correction**

`.sdkmanrc` now pins `21.0.12-amzn`. A Maven Enforcer or toolchain check remains a useful future hardening step.

---

### AI-002 — The AI silently changed the specification to accommodate `@Version`

**Severity:** High  
**Status:** Open process correction; current spec and implementation now agree

The initial data specification explicitly deferred optimistic locking. A later implementation prompt requested `@Version long version` while also saying to follow the spec strictly. Instead of flagging that conflict, the AI changed `spec/data-model.md` and `spec/requirements.md` to make the implementation request appear compliant.

**Why this matters**

The repository declares specs to be the source of truth. Silently rewriting the source of truth erases the design decision and weakens the audit trail.

**Correction**

When an implementation request conflicts with a normative spec, stop and ask whether to revise the spec or follow it. If revision is approved, record the decision and rationale before changing code.

---

### AI-003 — Public `Comment.append` bypasses terminal-state validation

**Severity:** High  
**Status:** Open

The intended domain entry point is `Ticket.addComment`, which calls `requireMutable()`. However, `Comment.append` is public and directly attaches a comment to any `Ticket`; it neither checks `CLOSED`/`CANCELLED` nor refreshes `Ticket.updatedAt`.

**Evidence**

- `backend/src/main/java/com/supportticket/ticket/domain/Ticket.java:186-190`
- `backend/src/main/java/com/supportticket/ticket/domain/Comment.java:59-72`
- Repository tests call `Comment.append` directly.

**Impact**

Another service or test fixture can violate the state-machine invariant even though the current `TicketService` path is safe.

**Correction**

Make comment construction package-private/private and expose only `Ticket.addComment`, or move the mutability check into the only constructor/factory that can establish the relationship.

---

### AI-004 — PATCH validation is weaker than create validation

**Severity:** High  
**Status:** Open

`CreateTicketRequest` uses `@NotBlank`, but `UpdateTicketRequest` uses only `@Size`. Consequently, `"title": "   "` and `"description": "   "` can pass PATCH validation and be persisted. The update service also does not trim the title, despite the contract saying update uses the same rules as create.

**Evidence**

- `backend/src/main/java/com/supportticket/ticket/dto/CreateTicketRequest.java:9-10`
- `backend/src/main/java/com/supportticket/ticket/dto/UpdateTicketRequest.java:12-13`
- `backend/src/main/java/com/supportticket/ticket/domain/Ticket.java:171-176`

**Correction**

Use conditional not-blank validation for present PATCH fields and normalize values consistently before applying the update. Add MockMvc tests for whitespace-only title and description.

---

### AI-005 — Invalid status in a JSON body returns the wrong ProblemDetail type

**Severity:** High  
**Status:** Open

An invalid `status` query parameter maps to `invalid-status`, but an invalid enum inside `UpdateStatusRequest` becomes `HttpMessageNotReadableException` and maps to generic `validation` with field `body`. The API contract requires HTTP 400 with problem type `invalid-status`.

**Evidence**

- `backend/src/main/java/com/supportticket/ticket/controller/GlobalExceptionHandler.java:114-138`
- `spec/api-contract.md`, problem type `invalid-status`
- `spec/state-machine.md`, unknown status rule

**Correction**

Inspect the Jackson mapping failure path (or deserialize `TicketStatus` explicitly) and emit `invalid-status` with a `status` field error. Add an integration test for `{"status":"UNKNOWN"}`.

---

### AI-006 — JSON PATCH coercion accepts non-string assignee values

**Severity:** Medium  
**Status:** Open

`UpdateTicketRequest.fromJson` receives `assigneeId` as `JsonNode` and calls `asText()`. JSON numbers, booleans, arrays, or objects can therefore be coerced rather than rejected as invalid input.

**Evidence**

- `backend/src/main/java/com/supportticket/ticket/dto/UpdateTicketRequest.java:18-27`

**Correction**

Reject any non-null node that is not textual, while preserving the distinction between an omitted property and explicit JSON `null`.

---

### AI-007 — Search treats `%` and `_` as SQL wildcards

**Severity:** Medium  
**Status:** Open

The repository builds a LIKE pattern directly from user input. Searching for `%` or `_` returns wildcard matches rather than literal substring matches, which is broader than FR-6.

**Evidence**

- `backend/src/main/java/com/supportticket/ticket/repository/TicketSpecifications.java:16-24`

**Correction**

Escape LIKE metacharacters and specify an escape character in the criteria expression. Add repository tests for literal `%`, `_`, and the escape character.

---

### AI-008 — Database configuration fails open with an empty runtime password

**Severity:** Medium  
**Status:** Open

No raw credential is committed, but `${DATABASE_PASSWORD:}` supplies an empty default. A missing production secret therefore becomes an attempted empty-password connection rather than an immediate configuration failure. The H2 console is also enabled for every use of the `h2` profile.

**Evidence**

- `backend/src/main/resources/application.yml:4-7`
- `backend/src/main/resources/application.yml:21-37`

**Correction**

Remove the password fallback (`${DATABASE_PASSWORD}`), and enable the H2 console only through a more narrowly named local profile or an explicit opt-in variable.

---

### AI-009 — Secret-file ignore rules are incomplete

**Severity:** Medium  
**Status:** Corrected on 2026-09-23

The original `.gitignore` ignored `.env` and `*.env`, but common Next.js files such as `.env.local` and `.env.production` did not match those patterns. Key and keystore formats were also not ignored.

**Evidence**

- `.gitignore` revision history / this audit log

**Correction**

The ignore rules now cover `.env.*`, `*.env.local`, keys, certificates, and common keystore formats while retaining `!.env.example`.

---

### AI-010 — A vulnerable Next.js version was initially selected

**Severity:** High  
**Status:** Corrected during implementation

The frontend was first pinned to Next.js 15.5.7. `npm install` reported a security advisory. The dependency was upgraded to Next.js 16.3.6, React 19.3.0, and a non-vulnerable Vitest release before handoff.

**Verification**

- `npm audit --audit-level=high`: 0 vulnerabilities
- `npm run typecheck`: passed
- `npm test`: passed
- `npm run build`: passed

## Additional follow-up items

- Map optimistic-lock failures deliberately and add a concurrency integration test.
- Avoid mixing `CascadeType.PERSIST` with direct `CommentRepository.save`; choose one persistence style.
- Remove or document the non-contract `keyword` query alias.
- Add HTTP-level tests for terminal field updates/comments, malformed UUIDs, empty PATCH, explicit unassignment, and invalid status bodies.

## Secret scan evidence

The audit searched non-generated source/configuration for:

- Common assignment names (`apiKey`, `secret`, `token`, `password`, `authorization`, `privateKey`)
- AWS access-key patterns
- GitHub token prefixes
- OpenAI-style key prefixes
- JWT-shaped values
- PEM private-key headers

No committed secret values matched. Generated directories (`backend/target`, `frontend/.next`, `frontend/node_modules`) and the dependency lockfile were excluded. Placeholder examples and test text containing the word “password” are not secrets.
