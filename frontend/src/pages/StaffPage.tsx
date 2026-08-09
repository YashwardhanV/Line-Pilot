import { FormEvent, useCallback, useEffect, useState } from "react";
import { CheckCircle2, History, LogOut, Play, SkipForward, UserRoundCheck } from "lucide-react";
import { ApiError, apiRequest, basicAuthorization } from "../api";
import { QueueBoard } from "../components/QueueBoard";
import { StatusBadge } from "../components/StatusBadge";
import { useQueueEvents } from "../hooks/useQueueEvents";
import type { AuthUser, HistoryPage, QueueSummary, TokenResponse } from "../types";

function messageFrom(error: unknown) {
  return error instanceof ApiError ? error.message : "The request could not be completed.";
}

export function StaffPage() {
  const [authorization, setAuthorization] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [username, setUsername] = useState("staff1");
  const [password, setPassword] = useState("demo123");
  const [queues, setQueues] = useState<QueueSummary[]>([]);
  const [selectedQueueId, setSelectedQueueId] = useState<number | null>(null);
  const [history, setHistory] = useState<HistoryPage | null>(null);
  const [historyPage, setHistoryPage] = useState(0);
  const [view, setView] = useState<"operate" | "history">("operate");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { snapshot, connectionState } = useQueueEvents(user ? selectedQueueId : null);

  const assignedToken = snapshot?.tokens.find(
    (token) => token.claimedBy === user?.displayName && (token.status === "CALLED" || token.status === "SERVING"),
  ) ?? null;

  const loadHistory = useCallback(async () => {
    if (!authorization || selectedQueueId === null) return;
    try {
      const data = await apiRequest<HistoryPage>(
        `/staff/queues/${selectedQueueId}/history?page=${historyPage}&size=10`,
        {},
        authorization,
      );
      setHistory(data);
    } catch (requestError) {
      setError(messageFrom(requestError));
    }
  }, [authorization, historyPage, selectedQueueId]);

  useEffect(() => {
    if (view === "history") void loadHistory();
  }, [loadHistory, view, snapshot?.generatedAt]);

  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const auth = basicAuthorization(username, password);
    try {
      const [currentUser, availableQueues] = await Promise.all([
        apiRequest<AuthUser>("/auth/me", {}, auth),
        apiRequest<QueueSummary[]>("/queues"),
      ]);
      setAuthorization(auth);
      setUser(currentUser);
      setQueues(availableQueues);
      setSelectedQueueId(availableQueues[0]?.id ?? null);
      setPassword("");
    } catch (requestError) {
      setError(messageFrom(requestError));
    } finally {
      setBusy(false);
    }
  }

  function signOut() {
    setAuthorization(null);
    setUser(null);
    setHistory(null);
    setPassword("demo123");
  }

  async function perform(path: string) {
    if (!authorization) return;
    setBusy(true);
    setError(null);
    try {
      await apiRequest<TokenResponse>(`/staff${path}`, { method: "POST" }, authorization);
      if (view === "history") await loadHistory();
    } catch (requestError) {
      setError(messageFrom(requestError));
    } finally {
      setBusy(false);
    }
  }

  if (!user) {
    return (
      <div className="mx-auto grid max-w-6xl items-stretch gap-6 px-5 py-10 lg:grid-cols-[1.05fr_0.95fr] lg:px-8 lg:py-16">
        <section className="relative overflow-hidden rounded-[2.5rem] bg-ink p-8 text-white shadow-card sm:p-12">
          <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full bg-violet/50 blur-3xl" />
          <p className="relative text-sm font-bold uppercase tracking-[0.2em] text-aqua">Staff workspace</p>
          <h1 className="relative mt-4 font-display text-5xl font-bold leading-tight tracking-[-0.04em]">One clear action at a time.</h1>
          <p className="relative mt-5 max-w-lg text-lg leading-8 text-white/60">
            Call the next customer, move the active token through its lifecycle, and keep every connected queue board current.
          </p>
          <div className="relative mt-8 rounded-3xl border border-white/10 bg-white/5 p-5 text-sm text-white/70">
            <p className="font-semibold text-white">Demo accounts</p>
            <p className="mt-2"><code>staff1</code> or <code>staff2</code> · password <code>demo123</code></p>
          </div>
        </section>
        <form onSubmit={signIn} className="flex flex-col justify-center rounded-[2.5rem] border border-white bg-white/90 p-7 shadow-card sm:p-10">
          <span className="grid h-12 w-12 place-items-center rounded-2xl bg-violet/10 text-violet"><UserRoundCheck size={22} /></span>
          <p className="mt-7 text-xs font-bold uppercase tracking-[0.2em] text-violet">Secure access</p>
          <h2 className="mt-2 font-display text-3xl font-bold">Open staff console</h2>
          <div className="mt-6 space-y-4">
            <div>
              <label htmlFor="staff-username" className="text-sm font-semibold">Username</label>
              <input id="staff-username" className="field mt-2" value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" required />
            </div>
            <div>
              <label htmlFor="staff-password" className="text-sm font-semibold">Password</label>
              <input id="staff-password" className="field mt-2" type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required />
            </div>
          </div>
          {error ? <p className="mt-4 text-sm font-medium text-rose-700" role="alert">{error}</p> : null}
          <button type="submit" className="button-primary mt-6 w-full" disabled={busy}>{busy ? "Signing in…" : "Open dashboard"}</button>
        </form>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-5 py-10 lg:px-8 lg:py-12">
      <div className="mb-8 flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-violet">Signed in as {user.displayName}</p>
          <h1 className="mt-1 font-display text-4xl font-bold tracking-tight">Service control room</h1>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <label htmlFor="queue-select" className="sr-only">Select queue</label>
          <select id="queue-select" className="field w-auto min-w-56" value={selectedQueueId ?? ""} onChange={(event) => { setSelectedQueueId(Number(event.target.value)); setHistoryPage(0); }}>
            {queues.map((queue) => <option key={queue.id} value={queue.id}>{queue.name}</option>)}
          </select>
          <button type="button" onClick={signOut} className="button-secondary"><LogOut size={17} /> Sign out</button>
        </div>
      </div>

      <div className="mb-6 flex gap-2 border-b border-ink/10" role="tablist" aria-label="Staff dashboard views">
        <button type="button" role="tab" aria-selected={view === "operate"} onClick={() => setView("operate")} className={`border-b-2 px-4 py-3 text-sm font-semibold ${view === "operate" ? "border-violet text-ink" : "border-transparent text-ink/50"}`}>Operate queue</button>
        <button type="button" role="tab" aria-selected={view === "history"} onClick={() => setView("history")} className={`border-b-2 px-4 py-3 text-sm font-semibold ${view === "history" ? "border-violet text-ink" : "border-transparent text-ink/50"}`}><span className="inline-flex items-center gap-2"><History size={16} /> History</span></button>
      </div>

      {error ? <p className="mb-5 rounded-2xl bg-rose-100 px-4 py-3 text-sm font-medium text-rose-800" role="alert">{error}</p> : null}

      {view === "operate" ? (
        <div className="grid items-start gap-6 lg:grid-cols-[0.65fr_1.35fr]">
          <section className="rounded-[2rem] bg-ink p-6 text-white shadow-card lg:p-8" aria-labelledby="operator-controls-title">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-aqua">Operator controls</p>
            <h2 id="operator-controls-title" className="mt-2 font-display text-2xl font-bold">Current assignment</h2>
            {assignedToken ? (
              <div className="mt-6 rounded-3xl bg-white p-6 text-ink">
                <div className="flex items-start justify-between gap-3">
                  <p className="font-display text-4xl font-bold">{assignedToken.displayNumber}</p>
                  <StatusBadge status={assignedToken.status} />
                </div>
                <p className="mt-3 text-sm text-ink/50">This token is transactionally locked to {user.displayName}.</p>
              </div>
            ) : (
              <div className="mt-6 rounded-3xl border border-dashed border-white/20 p-6 text-sm text-white/50">No active token assigned to you.</div>
            )}

            <div className="mt-6 grid gap-3">
              {!assignedToken ? (
                <button type="button" className="button-primary" disabled={busy || selectedQueueId === null} onClick={() => void perform(`/queues/${selectedQueueId}/call-next`)}>
                  <SkipForward size={18} /> Call next
                </button>
              ) : null}
              {assignedToken?.status === "CALLED" ? (
                <>
                  <button type="button" className="button-primary" disabled={busy} onClick={() => void perform(`/tokens/${assignedToken.id}/start`)}><Play size={18} /> Start service</button>
                  <button type="button" className="button-secondary text-rose-700" disabled={busy} onClick={() => void perform(`/tokens/${assignedToken.id}/skip`)}><SkipForward size={18} /> Mark no-show</button>
                </>
              ) : null}
              {assignedToken?.status === "SERVING" ? (
                <button type="button" className="button-primary" disabled={busy} onClick={() => void perform(`/tokens/${assignedToken.id}/complete`)}><CheckCircle2 size={18} /> Complete service</button>
              ) : null}
            </div>
          </section>
          <QueueBoard snapshot={snapshot} connectionState={connectionState} />
        </div>
      ) : (
        <section className="overflow-hidden rounded-[2rem] bg-white shadow-card" aria-labelledby="history-title">
          <div className="flex items-center justify-between gap-4 border-b border-ink/10 p-6 lg:p-8">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.2em] text-ink/40">Audit trail</p>
              <h2 id="history-title" className="mt-1 font-display text-2xl font-bold">Queue history</h2>
            </div>
            <span className="text-sm text-ink/50">{history?.totalElements ?? 0} records</span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="bg-canvas/70 text-xs uppercase tracking-wider text-ink/45">
                <tr><th className="px-6 py-4">Token</th><th className="px-6 py-4">Customer</th><th className="px-6 py-4">Status</th><th className="px-6 py-4">Staff</th><th className="px-6 py-4">Joined</th></tr>
              </thead>
              <tbody className="divide-y divide-ink/10">
                {history?.content.map((item) => (
                  <tr key={item.id}>
                    <td className="px-6 py-4 font-display text-lg font-bold">{item.displayNumber}</td>
                    <td className="px-6 py-4">{item.customerName}</td>
                    <td className="px-6 py-4"><StatusBadge status={item.status} /></td>
                    <td className="px-6 py-4 text-ink/60">{item.claimedBy ?? "—"}</td>
                    <td className="px-6 py-4 text-ink/60">{new Date(item.joinedAt).toLocaleString()}</td>
                  </tr>
                ))}
                {history?.content.length === 0 ? <tr><td colSpan={5} className="px-6 py-12 text-center text-ink/45">No history yet.</td></tr> : null}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between border-t border-ink/10 p-5">
            <button type="button" className="button-secondary py-2" disabled={!history || history.page === 0} onClick={() => setHistoryPage((page) => Math.max(0, page - 1))}>Previous</button>
            <span className="text-sm text-ink/50">Page {(history?.page ?? 0) + 1} of {Math.max(history?.totalPages ?? 1, 1)}</span>
            <button type="button" className="button-secondary py-2" disabled={!history || history.page + 1 >= history.totalPages} onClick={() => setHistoryPage((page) => page + 1)}>Next</button>
          </div>
        </section>
      )}
    </div>
  );
}
