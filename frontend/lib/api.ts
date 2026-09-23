export type TicketStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "CANCELLED";

export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export interface TicketSummary {
  id: string;
  title: string;
  priority: Priority;
  status: TicketStatus;
  assigneeId: string | null;
  reporterId: string;
  createdAt: string;
  updatedAt: string;
}

export interface Comment {
  id: string;
  ticketId: string;
  authorId: string;
  body: string;
  createdAt: string;
}

export interface Ticket extends TicketSummary {
  description: string;
  resolvedAt: string | null;
  closedAt: string | null;
  comments: Comment[];
}

export interface TicketPage {
  content: TicketSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProblemField {
  field: string;
  message: string;
}

export interface ApiProblem {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  errors?: ProblemField[];
}

export class ApiError extends Error {
  constructor(public readonly problem: ApiProblem) {
    super(problem.detail);
  }
}

export const transitions: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["RESOLVED", "CANCELLED"],
  RESOLVED: ["CLOSED"],
  CLOSED: [],
  CANCELLED: [],
};

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      Accept: "application/json, application/problem+json",
      ...(init?.body ? { "Content-Type": "application/json" } : {}),
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let problem: ApiProblem;
    try {
      problem = (await response.json()) as ApiProblem;
    } catch {
      problem = {
        type: "about:blank",
        title: "Request failed",
        status: response.status,
        detail: `The server returned HTTP ${response.status}.`,
      };
    }
    throw new ApiError(problem);
  }

  return (await response.json()) as T;
}

export function listTickets(params: {
  q?: string;
  status?: TicketStatus | "";
  page?: number;
  size?: number;
}): Promise<TicketPage> {
  const query = new URLSearchParams();
  if (params.q) query.set("q", params.q);
  if (params.status) query.set("status", params.status);
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 20));
  return request<TicketPage>(`/api/v1/tickets?${query}`);
}

export const api = {
  getTicket: (id: string) => request<Ticket>(`/api/v1/tickets/${id}`),
  createTicket: (body: {
    title: string;
    description: string;
    priority: Priority;
    assigneeId?: string | null;
    reporterId: string;
  }) => request<Ticket>("/api/v1/tickets", { method: "POST", body: JSON.stringify(body) }),
  updateTicket: (
    id: string,
    body: Partial<Pick<Ticket, "title" | "description" | "priority" | "assigneeId">>,
  ) => request<Ticket>(`/api/v1/tickets/${id}`, { method: "PATCH", body: JSON.stringify(body) }),
  updateStatus: (id: string, status: TicketStatus) =>
    request<Ticket>(`/api/v1/tickets/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),
  addComment: (id: string, body: { authorId: string; body: string }) =>
    request<Comment>(`/api/v1/tickets/${id}/comments`, {
      method: "POST",
      body: JSON.stringify(body),
    }),
};
