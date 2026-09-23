# Prompt History

Session date: 2026-09-22 to 2026-09-23  
Project: Support Ticket Management System

This is a concise audit history of the user prompts and the implementation outcomes from this session. It summarizes intent and results without reproducing the full conversation verbatim.

## 1. Project steering and rule setup

**User goal:** Establish Cursor rules before application code.

**Requested**

- Java 21 and Spring Boot 3 conventions
- Layered Controller → Service → Repository → Domain architecture
- Record DTOs, RFC 7807, PostgreSQL/H2
- `/api/v1` and status-code standards
- JUnit 5, Mockito, MockMvc, and parameterized state tests

**Outcome**

- Created `.cursor/rules/java-springboot.mdc`
- Created `.cursor/rules/api-standards.mdc`
- Created `.cursor/rules/testing.mdc`

The files use Cursor’s `.mdc` format and scoped globs.

## 2. Core specification definition

**User goal:** Make `spec/` the implementation source of truth.

**Requested**

- Functional requirements
- Ticket state machine
- REST API contract
- JPA data model

**Outcome**

- Expanded `spec/requirements.md`
- Expanded `spec/state-machine.md`
- Expanded `spec/api-contract.md`
- Expanded `spec/data-model.md`

The specs define ticket/comment identifiers, pagination, search/filter behavior, transition rules, DTO shapes, ProblemDetail types, persistence fields, and indexes.

## 3. State machine and domain model

**User goal:** Implement Java 21 domain entities with encapsulated transition validation.

**Requested**

- `TicketStatus.canTransitionTo`
- JPA `Ticket` with `transitionTo`
- Custom `InvalidStateTransitionException`
- JPA `Comment` relationship
- `@Version`

**Outcome**

- Added the Maven backend module
- Added `TicketStatus`, `Priority`, `Ticket`, `Comment`, and `InvalidStateTransitionException`
- Added domain tests for legal/illegal transitions and timestamp side effects
- Added `@Version long version`

**Audit note:** The request for `@Version` conflicted with the then-current spec. The AI silently revised the spec instead of surfacing that decision; see `docs/ai-corrections-log.md` (AI-002).

## 4. Database persistence and repositories

**User goal:** Configure PostgreSQL/H2 and searchable Spring Data repositories.

**Requested**

- PostgreSQL runtime and H2 testing configuration
- Ticket keyword search and status filtering
- Ordered comment retrieval

**Outcome**

- Added `application.yml`
- Added Flyway migration `V1__create_tickets_and_comments.sql`
- Added `TicketRepository`, `TicketSpecifications`, and `CommentRepository`
- Added JPA slice tests for search, status filters, composition, and comment ordering

## 5. Service layer and RFC 7807 handling

**User goal:** Add transactions, use cases, and global exception mapping.

**Requested**

- Resource-not-found and invalid-transition errors
- `GlobalExceptionHandler`
- Ticket create/get/list/update/status/comment methods
- Transaction boundaries

**Outcome**

- Added `TicketService` and record response mapping
- Added `ResourceNotFoundException`, request validation, and terminal mutability errors
- Added `GlobalExceptionHandler` returning `ProblemDetail`
- Added service and handler tests

## 6. REST controller implementation

**User goal:** Expose the `/api/v1/tickets` API with immutable records and strict validation.

**Requested**

- Create/update/status/comment request records
- Ticket/comment response records
- Create, list, details, update, status, and comment endpoints

**Outcome**

- Added `TicketController`
- Added immutable API records
- Added `Location` headers for ticket/comment creation
- Added MockMvc controller tests

**Audit notes:** PATCH blank validation and invalid status body handling are incomplete; see AI-004 and AI-005.

## 7. Backend unit and integration tests

**User goal:** Prove legal transitions succeed and illegal transitions are rejected through the domain and HTTP API.

**Requested**

- Parameterized tests for every transition
- Full create/status/comment API flow
- RFC 7807 HTTP 422 and validation HTTP 400

**Outcome**

- Expanded `TicketStatusTest` to cover all 25 matrix cells
- Added `TicketControllerIntegrationTest` with Spring Boot, MockMvc, and H2
- Verified create → in-progress → comment flow
- Verified illegal reopen cases return 422 and do not mutate state
- Verified invalid create input returns 400

At that point the backend suite reported 95 passing tests.

## 8. Next.js / React frontend

**User goal:** Build the ticket-management UI with state controls and API error display.

**Outcome**

- Added a Next.js 16 / React 19 application under `frontend/`
- Added queue list, search, status filtering, pagination, and ticket creation
- Added ticket detail/edit/comment workflows
- Added legal state-transition controls and terminal-state locking
- Added RFC 7807 and field-error rendering
- Added a same-origin Next.js proxy for `/api/v1`
- Added frontend state-control tests and production build validation

**Validation**

- TypeScript passed
- Frontend tests passed
- Production build passed
- Dependency audit reported zero vulnerabilities

**Audit note:** A vulnerable Next.js version was initially selected and then corrected before handoff; see AI-010.

## 9. OpenSpec verification and audit trail

**User goal:** Audit implementation compliance, scan for secrets, capture AI mistakes, and preserve prompt history.

**Actions**

- Reviewed backend architecture and state behavior against Cursor steering and `spec/`
- Scanned non-generated source/configuration for common key, token, credential, JWT, and private-key patterns
- Recorded implementation mistakes in `docs/ai-corrections-log.md`
- Created this prompt-history summary
- Re-ran backend and frontend validation
- Produced a standalone visual audit canvas

## Current audit conclusion

The primary architecture and ticket transition matrix are implemented correctly. No committed secret values were found. The highest-priority open corrections are:

1. Close the public `Comment.append` domain bypass.
2. Align PATCH blank/trim validation with create.
3. Return `invalid-status` for invalid status JSON bodies.
4. Remove the empty runtime database-password default.

The Java 21 SDKMAN pin, stale Kiro documentation, and secret-file ignore patterns were corrected during the repository cleanup on 2026-09-23.

## 10. Repository cleanup

**User goal:** Remove stale documentation, Kiro remnants, generated agent guidance, and unnecessary files.

**Outcome**

- Replaced the stale README with project-specific layout, startup, PostgreSQL, and verification guidance.
- Removed `.kiroignore`, generated frontend `AGENTS.md`/`CLAUDE.md`, an empty duplicate audit file, and an empty `scripts/` directory.
- Disabled Next.js agent-rule generation and aligned `.sdkmanrc` with Java 21.
- Expanded `.gitignore` for local environment files, keys, certificates, and keystores.
- Retained ignored build/dependency directories because the running application and validation commands use them.

## 11. Backend package naming

**User goal:** Use package names that describe each backend layer directly.

**Outcome**

- Renamed `com.supportticket.ticket.api` to `com.supportticket.ticket.dto`.
- Renamed `com.supportticket.ticket.web` to `com.supportticket.ticket.controller`.
- Updated source directories, test packages, imports, and documentation references.
