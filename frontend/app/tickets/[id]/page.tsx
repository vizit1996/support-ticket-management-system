"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import {
  ApiError,
  ApiProblem,
  Priority,
  Ticket,
  TicketStatus,
  api,
  transitions,
} from "@/lib/api";

const priorities: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];

function ErrorNotice({ error, onClose }: { error: ApiProblem; onClose: () => void }) {
  return (
    <div className="error-notice" role="alert">
      <div>
        <strong>{error.title}</strong>
        <p>{error.detail}</p>
        {error.errors?.map((item) => (
          <small key={`${item.field}-${item.message}`}>{item.field}: {item.message}</small>
        ))}
      </div>
      <button className="icon-button" type="button" onClick={onClose} aria-label="Dismiss error">×</button>
    </div>
  );
}

export default function TicketDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState<ApiProblem | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setTicket(await api.getTicket(id));
    } catch (caught) {
      setError(problem(caught, "Could not load ticket"));
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  async function changeStatus(status: TicketStatus) {
    setBusy(true);
    setError(null);
    try {
      setTicket(await api.updateStatus(id, status));
    } catch (caught) {
      setError(problem(caught, "Status update failed"));
    } finally {
      setBusy(false);
    }
  }

  async function saveEdit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    setBusy(true);
    setError(null);
    try {
      setTicket(
        await api.updateTicket(id, {
          title: String(form.get("title")),
          description: String(form.get("description")),
          priority: form.get("priority") as Priority,
          assigneeId: String(form.get("assigneeId")).trim() || null,
        }),
      );
      setEditing(false);
    } catch (caught) {
      setError(problem(caught, "Ticket update failed"));
    } finally {
      setBusy(false);
    }
  }

  async function addComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    setBusy(true);
    setError(null);
    try {
      await api.addComment(id, {
        authorId: String(form.get("authorId")),
        body: String(form.get("body")),
      });
      formElement.reset();
      setTicket(await api.getTicket(id));
    } catch (caught) {
      setError(problem(caught, "Comment could not be added"));
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <main className="page-shell"><div className="empty-state">Loading ticket…</div></main>;

  if (!ticket) {
    return (
      <main className="page-shell">
        {error && <ErrorNotice error={error} onClose={() => setError(null)} />}
        <Link className="button quiet" href="/">← Return to queue</Link>
      </main>
    );
  }

  const mutable = ticket.status !== "CLOSED" && ticket.status !== "CANCELLED";

  return (
    <main className="page-shell detail-shell">
      <Link className="back-link" href="/">← Back to tickets</Link>
      {error && <ErrorNotice error={error} onClose={() => setError(null)} />}

      <section className="detail-header">
        <div>
          <div className="detail-meta">
            <span>#{ticket.id.slice(0, 8)}</span>
            <span className={`badge status-${ticket.status.toLowerCase()}`}>{ticket.status.replace("_", " ")}</span>
            <span className={`priority priority-${ticket.priority.toLowerCase()}`}>{ticket.priority}</span>
          </div>
          <h1>{ticket.title}</h1>
          <p>Opened by {ticket.reporterId} on {formatDate(ticket.createdAt)}</p>
        </div>
        <div className="status-actions">
          {transitions[ticket.status].map((status) => (
            <button
              className={status === "CANCELLED" ? "button danger" : "button primary"}
              disabled={busy}
              onClick={() => void changeStatus(status)}
              key={status}
            >
              {status === "IN_PROGRESS" ? "Start progress" : status === "RESOLVED" ? "Resolve" : status === "CLOSED" ? "Close" : "Cancel"}
            </button>
          ))}
        </div>
      </section>

      <div className="detail-grid">
        <div className="detail-main-column">
          <section className="panel detail-panel">
            <div className="panel-heading">
              <h2>Request details</h2>
              {mutable && !editing && <button className="text-button" onClick={() => setEditing(true)}>Edit</button>}
            </div>
            {editing ? (
              <form className="form-stack" onSubmit={saveEdit}>
                <label>Title<input name="title" defaultValue={ticket.title} required maxLength={200} /></label>
                <label>Description<textarea name="description" defaultValue={ticket.description} required maxLength={10000} rows={7} /></label>
                <div className="form-grid">
                  <label>
                    Priority
                    <select name="priority" defaultValue={ticket.priority}>
                      {priorities.map((value) => <option key={value}>{value}</option>)}
                    </select>
                  </label>
                  <label>Assignee<input name="assigneeId" defaultValue={ticket.assigneeId ?? ""} maxLength={128} /></label>
                </div>
                <div className="form-actions">
                  <button type="button" className="button quiet" onClick={() => setEditing(false)}>Cancel</button>
                  <button className="button primary" disabled={busy}>{busy ? "Saving…" : "Save changes"}</button>
                </div>
              </form>
            ) : (
              <p className="description">{ticket.description}</p>
            )}
          </section>

          <section className="panel detail-panel">
            <div className="panel-heading">
              <div><h2>Conversation</h2><span>{ticket.comments.length} comments</span></div>
            </div>
            <div className="comments">
              {ticket.comments.length === 0 && <div className="empty-state compact">No comments yet.</div>}
              {ticket.comments.map((comment) => (
                <article className="comment" key={comment.id}>
                  <span className="avatar">{comment.authorId.slice(0, 1).toUpperCase()}</span>
                  <div>
                    <div className="comment-heading">
                      <strong>{comment.authorId}</strong>
                      <time>{formatDate(comment.createdAt)}</time>
                    </div>
                    <p>{comment.body}</p>
                  </div>
                </article>
              ))}
            </div>
            {mutable ? (
              <form className="comment-form" onSubmit={addComment}>
                <div className="form-grid">
                  <label>Author<input name="authorId" required maxLength={128} placeholder="agent@example.com" /></label>
                </div>
                <label>Reply<textarea name="body" required maxLength={5000} rows={4} placeholder="Write an update…" /></label>
                <div className="form-actions"><button className="button primary" disabled={busy}>Add comment</button></div>
              </form>
            ) : (
              <div className="locked-note">This ticket is {ticket.status.toLowerCase()} and no longer accepts updates.</div>
            )}
          </section>
        </div>

        <aside className="panel sidebar">
          <h2>Ticket information</h2>
          <dl>
            <div><dt>Status</dt><dd>{ticket.status.replace("_", " ")}</dd></div>
            <div><dt>Priority</dt><dd>{ticket.priority}</dd></div>
            <div><dt>Assignee</dt><dd>{ticket.assigneeId ?? "Unassigned"}</dd></div>
            <div><dt>Reporter</dt><dd>{ticket.reporterId}</dd></div>
            <div><dt>Updated</dt><dd>{formatDate(ticket.updatedAt)}</dd></div>
            {ticket.resolvedAt && <div><dt>Resolved</dt><dd>{formatDate(ticket.resolvedAt)}</dd></div>}
            {ticket.closedAt && <div><dt>Closed</dt><dd>{formatDate(ticket.closedAt)}</dd></div>}
          </dl>
        </aside>
      </div>
    </main>
  );
}

function problem(caught: unknown, title: string): ApiProblem {
  return caught instanceof ApiError
    ? caught.problem
    : { type: "about:blank", title, status: 0, detail: "Please try again." };
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
