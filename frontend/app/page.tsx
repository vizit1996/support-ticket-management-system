"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  ApiError,
  ApiProblem,
  Priority,
  TicketPage,
  TicketStatus,
  api,
  listTickets,
} from "@/lib/api";

const statuses: TicketStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "CANCELLED"];
const priorities: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];

function ErrorNotice({ error, onClose }: { error: ApiProblem; onClose: () => void }) {
  return (
    <div className="error-notice" role="alert">
      <div>
        <strong>{error.title}</strong>
        <p>{error.detail}</p>
        {error.errors?.map((item) => (
          <small key={`${item.field}-${item.message}`}>
            {item.field}: {item.message}
          </small>
        ))}
      </div>
      <button type="button" className="icon-button" onClick={onClose} aria-label="Dismiss error">
        ×
      </button>
    </div>
  );
}

export default function Dashboard() {
  const [tickets, setTickets] = useState<TicketPage | null>(null);
  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<TicketStatus | "">("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiProblem | null>(null);
  const [showCreate, setShowCreate] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setTickets(await listTickets({ q: query, status, page }));
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.problem
          : { type: "about:blank", title: "Could not load tickets", status: 0, detail: "Try again." },
      );
    } finally {
      setLoading(false);
    }
  }, [page, query, status]);

  useEffect(() => {
    void load();
  }, [load]);

  function search(event: FormEvent) {
    event.preventDefault();
    setPage(0);
    setQuery(queryInput.trim());
  }

  return (
    <main className="page-shell">
      <section className="hero">
        <div>
          <p className="eyebrow">Workspace / Tickets</p>
          <h1>Support queue</h1>
          <p className="subtle">Track requests, move work forward, and keep every update in one place.</p>
        </div>
        <button className="button primary" onClick={() => setShowCreate(true)}>
          <span aria-hidden>＋</span> New ticket
        </button>
      </section>

      {error && <ErrorNotice error={error} onClose={() => setError(null)} />}

      <section className="panel">
        <div className="toolbar">
          <form className="search" onSubmit={search}>
            <span aria-hidden>⌕</span>
            <input
              value={queryInput}
              onChange={(event) => setQueryInput(event.target.value)}
              placeholder="Search title or description"
              maxLength={200}
              aria-label="Search tickets"
            />
            <button className="button quiet" type="submit">
              Search
            </button>
          </form>
          <select
            value={status}
            onChange={(event) => {
              setPage(0);
              setStatus(event.target.value as TicketStatus | "");
            }}
            aria-label="Filter by status"
          >
            <option value="">All statuses</option>
            {statuses.map((value) => (
              <option key={value} value={value}>
                {value.replace("_", " ")}
              </option>
            ))}
          </select>
        </div>

        <div className="panel-heading">
          <div>
            <h2>{status ? status.replace("_", " ") : "All tickets"}</h2>
            <span>{tickets?.totalElements ?? 0} requests</span>
          </div>
          {(query || status) && (
            <button
              className="text-button"
              onClick={() => {
                setQuery("");
                setQueryInput("");
                setStatus("");
                setPage(0);
              }}
            >
              Clear filters
            </button>
          )}
        </div>

        {loading ? (
          <div className="empty-state">Loading ticket queue…</div>
        ) : tickets?.content.length ? (
          <div className="ticket-list">
            {tickets.content.map((ticket) => (
              <Link href={`/tickets/${ticket.id}`} className="ticket-row" key={ticket.id}>
                <div className={`priority-marker priority-${ticket.priority.toLowerCase()}`} />
                <div className="ticket-main">
                  <strong>{ticket.title}</strong>
                  <span>
                    #{ticket.id.slice(0, 8)} · opened by {ticket.reporterId}
                  </span>
                </div>
                <span className={`badge status-${ticket.status.toLowerCase()}`}>
                  {ticket.status.replace("_", " ")}
                </span>
                <span className={`priority priority-${ticket.priority.toLowerCase()}`}>{ticket.priority}</span>
                <div className="assignee">
                  <span className="avatar">{ticket.assigneeId?.slice(0, 1).toUpperCase() ?? "–"}</span>
                  <span>{ticket.assigneeId ?? "Unassigned"}</span>
                </div>
                <time>{new Date(ticket.updatedAt).toLocaleDateString()}</time>
                <span className="chevron">›</span>
              </Link>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <strong>No tickets found</strong>
            <span>Try another filter or create the first request.</span>
          </div>
        )}

        {tickets && tickets.totalPages > 1 && (
          <div className="pagination">
            <button className="button quiet" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
              Previous
            </button>
            <span>
              Page {tickets.page + 1} of {tickets.totalPages}
            </span>
            <button
              className="button quiet"
              disabled={page + 1 >= tickets.totalPages}
              onClick={() => setPage((value) => value + 1)}
            >
              Next
            </button>
          </div>
        )}
      </section>

      {showCreate && (
        <CreateTicketModal
          onClose={() => setShowCreate(false)}
          onCreated={() => {
            setShowCreate(false);
            void load();
          }}
        />
      )}
    </main>
  );
}

function CreateTicketModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<ApiProblem | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setSaving(true);
    setError(null);
    try {
      await api.createTicket({
        title: String(form.get("title")),
        description: String(form.get("description")),
        priority: form.get("priority") as Priority,
        assigneeId: String(form.get("assigneeId")).trim() || null,
        reporterId: String(form.get("reporterId")),
      });
      onCreated();
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.problem
          : { type: "about:blank", title: "Could not create ticket", status: 0, detail: "Try again." },
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section className="modal" role="dialog" aria-modal="true" aria-labelledby="create-title" onMouseDown={(e) => e.stopPropagation()}>
        <div className="modal-heading">
          <div>
            <p className="eyebrow">New request</p>
            <h2 id="create-title">Create a ticket</h2>
          </div>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
        {error && <ErrorNotice error={error} onClose={() => setError(null)} />}
        <form className="form-stack" onSubmit={submit}>
          <label>
            Title
            <input name="title" required minLength={1} maxLength={200} autoFocus />
          </label>
          <label>
            Description
            <textarea name="description" required minLength={1} maxLength={10000} rows={5} />
          </label>
          <div className="form-grid">
            <label>
              Priority
              <select name="priority" defaultValue="MEDIUM">
                {priorities.map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </label>
            <label>
              Reporter
              <input name="reporterId" required maxLength={128} placeholder="user@example.com" />
            </label>
          </div>
          <label>
            Assignee <span className="optional">optional</span>
            <input name="assigneeId" maxLength={128} placeholder="agent@example.com" />
          </label>
          <div className="form-actions">
            <button type="button" className="button quiet" onClick={onClose}>
              Cancel
            </button>
            <button className="button primary" disabled={saving}>
              {saving ? "Creating…" : "Create ticket"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
