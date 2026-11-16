const API_BASE = import.meta.env.VITE_API_BASE ?? "/api";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export function basicAuthorization(username: string, password: string): string {
  return `Basic ${btoa(`${username}:${password}`)}`;
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  authorization?: string,
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.body) {
    headers.set("Content-Type", "application/json");
  }
  if (authorization) {
    headers.set("Authorization", authorization);
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (!response.ok) {
    const problem = (await response.json().catch(() => null)) as
      | { detail?: string; errors?: Record<string, string> }
      | null;
    // For validation errors, the field message ("Phone must be in international format…") is more useful.
    const fieldError = problem?.errors ? Object.values(problem.errors)[0] : undefined;
    throw new ApiError(response.status, fieldError ?? problem?.detail ?? `Request failed with status ${response.status}`);
  }
  return (await response.json()) as T;
}

export function eventUrl(queueId: number): string {
  return `${API_BASE}/queues/${queueId}/events`;
}
