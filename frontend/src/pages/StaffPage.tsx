import { FormEvent, useCallback, useEffect, useState } from "react";
import { History, LogOut } from "lucide-react";
import { ApiError, apiRequest, basicAuthorization } from "../api";
import { QueueBoard } from "../components/QueueBoard";
import { HistoryTable } from "../components/staff/HistoryTable";
import { OperatorPanel } from "../components/staff/OperatorPanel";
import { StaffLogin } from "../components/staff/StaffLogin";
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

  // The live snapshot already tells us which token (if any) this staff member is handling.
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
      <StaffLogin
        username={username}
        password={password}
        onUsernameChange={setUsername}
        onPasswordChange={setPassword}
        onSubmit={signIn}
        busy={busy}
        error={error}
      />
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
          <OperatorPanel
            assignedToken={assignedToken}
            staffName={user.displayName}
            queueId={selectedQueueId}
            busy={busy}
            onAction={(path) => void perform(path)}
          />
          <QueueBoard snapshot={snapshot} connectionState={connectionState} />
        </div>
      ) : (
        <HistoryTable
          history={history}
          onPrevious={() => setHistoryPage((page) => Math.max(0, page - 1))}
          onNext={() => setHistoryPage((page) => page + 1)}
        />
      )}
    </div>
  );
}
