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

CREATE TABLE comments (
    id          UUID PRIMARY KEY,
    ticket_id   UUID NOT NULL,
    author_id   VARCHAR(128) NOT NULL,
    body        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE RESTRICT
);

CREATE INDEX idx_comments_ticket_id_created_at
    ON comments (ticket_id, created_at ASC);
