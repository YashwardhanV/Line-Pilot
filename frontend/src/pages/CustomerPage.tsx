import { ArrowRight, Clock3, MapPin, Radio, ShieldCheck, UsersRound } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";
import { ApiError, apiRequest } from "../api";
import { QueueBoard } from "../components/QueueBoard";
import { TicketCard } from "../components/TicketCard";
import { useQueueEvents } from "../hooks/useQueueEvents";
import type { QueueSummary, TokenResponse } from "../types";

const TRACKING_KEY = "linepilot:token-public-id:v1";
const JOIN_STEPS = [["01", "Choose"], ["02", "Join"], ["03", "Track live"]] as const;

function messageFrom(error: unknown) {
  return error instanceof ApiError ? error.message : "Something went wrong. Please try again.";
}

export function CustomerPage() {
  const [queues, setQueues] = useState<QueueSummary[]>([]);
  const [selectedQueueId, setSelectedQueueId] = useState<number | null>(null);
  const [customerName, setCustomerName] = useState("");
  const [token, setToken] = useState<TokenResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { snapshot, connectionState } = useQueueEvents(selectedQueueId);
  const snapshotGeneratedAt = snapshot?.generatedAt;
  const snapshotQueueId = snapshot?.queueId;
  const trackedPublicId = token?.publicId;
  const trackedQueueId = token?.queueId;

  useEffect(() => {
    apiRequest<QueueSummary[]>("/queues")
      .then((data) => {
        setQueues(data);
        setSelectedQueueId((current) => current ?? data[0]?.id ?? null);
      })
      .catch((requestError: unknown) => setError(messageFrom(requestError)));

    const publicId = localStorage.getItem(TRACKING_KEY);
    if (publicId) {
      apiRequest<TokenResponse>(`/tokens/${publicId}`)
        .then((storedToken) => setToken(storedToken))
        .catch(() => localStorage.removeItem(TRACKING_KEY));
    }
  }, []);

  useEffect(() => {
    if (!trackedPublicId || !snapshotGeneratedAt || trackedQueueId !== snapshotQueueId) return;
    apiRequest<TokenResponse>(`/tokens/${trackedPublicId}`)
      .then(setToken)
      .catch(() => undefined);
  }, [snapshotGeneratedAt, snapshotQueueId, trackedPublicId, trackedQueueId]);

  async function joinQueue(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (selectedQueueId === null) return;
    setSubmitting(true);
    setError(null);
    try {
      const joined = await apiRequest<TokenResponse>(`/queues/${selectedQueueId}/tokens`, {
        method: "POST",
        body: JSON.stringify({ customerName }),
      });
      setToken(joined);
      setCustomerName("");
      localStorage.setItem(TRACKING_KEY, joined.publicId);
    } catch (requestError) {
      setError(messageFrom(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  async function cancelToken() {
    if (!token) return;
    setSubmitting(true);
    setError(null);
    try {
      const cancelled = await apiRequest<TokenResponse>(`/tokens/${token.publicId}/cancel`, { method: "POST" });
      setToken(cancelled);
      localStorage.removeItem(TRACKING_KEY);
    } catch (requestError) {
      setError(messageFrom(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-7xl px-5 py-8 lg:px-8 lg:py-12">
      <section className="relative overflow-hidden rounded-[2.5rem] bg-ink px-6 py-8 text-white shadow-card sm:px-10 sm:py-12 lg:px-14 lg:py-14">
        <div className="absolute -right-32 -top-32 h-80 w-80 rounded-full bg-violet/50 blur-3xl" />
        <div className="absolute -bottom-32 left-1/3 h-72 w-72 rounded-full bg-cyan-400/20 blur-3xl" />

        <div className="relative grid items-center gap-10 lg:grid-cols-[0.9fr_1.1fr] lg:gap-14">
          <div>
            <p className="mb-6 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-4 py-2 text-sm font-semibold text-aqua">
              <Radio size={15} aria-hidden="true" /> Your place updates live
            </p>
            <h1 className="max-w-2xl font-display text-5xl font-bold leading-[0.98] tracking-[-0.055em] sm:text-6xl lg:text-7xl">
              Your turn,<br /><span className="text-aqua">without the wait.</span>
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-8 text-white/65">
              Choose a service, get a numbered token, and carry on with your day while LinePilot keeps you updated.
            </p>
            <ol className="mt-9 grid gap-3 text-sm sm:grid-cols-3" aria-label="How LinePilot works">
              {JOIN_STEPS.map(([step, label]) => (
                <li key={step} className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
                  <span className="font-display text-xs font-bold text-aqua">{step}</span>
                  <span className="font-semibold text-white/80">{label}</span>
                </li>
              ))}
            </ol>
          </div>

          <div className="rounded-[2rem] bg-white p-5 text-ink shadow-2xl sm:p-7">
            <div className="flex items-start justify-between gap-4">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.2em] text-violet">Get a token</p>
                <h2 className="mt-2 font-display text-2xl font-bold">Select your service</h2>
              </div>
              <span className="rounded-full bg-aqua/55 px-3 py-1.5 text-xs font-bold">No account needed</span>
            </div>

            <div className="mt-6 grid gap-3 sm:grid-cols-2">
              {queues.map((queue) => (
                <button
                  key={queue.id}
                  type="button"
                  onClick={() => setSelectedQueueId(queue.id)}
                  className={`rounded-2xl border p-4 text-left transition ${selectedQueueId === queue.id ? "border-violet bg-violet/5 ring-2 ring-violet/10" : "border-ink/10 bg-canvas/60 hover:border-violet/40"}`}
                  aria-pressed={selectedQueueId === queue.id}
                >
                  <div className="flex items-start justify-between gap-3">
                    <span className="font-display font-bold">{queue.name}</span>
                    <span className={`mt-1 h-2.5 w-2.5 rounded-full ${queue.open ? "bg-emerald-500" : "bg-stone-400"}`} aria-label={queue.open ? "Open" : "Closed"} />
                  </div>
                  <p className="mt-2 flex items-center gap-1.5 text-sm text-ink/50"><MapPin size={14} /> {queue.location}</p>
                  <div className="mt-4 flex gap-4 text-sm font-semibold text-ink/70">
                    <span className="flex items-center gap-1.5"><UsersRound size={15} /> {queue.waitingCount}</span>
                    <span className="flex items-center gap-1.5"><Clock3 size={15} /> ~{queue.estimatedWaitMinutes} min</span>
                  </div>
                </button>
              ))}
            </div>

            <form onSubmit={joinQueue} className="mt-6 border-t border-ink/10 pt-6">
              <label htmlFor="customer-name" className="text-sm font-semibold">Your name</label>
              <div className="mt-2 flex flex-col gap-3 sm:flex-row">
                <input
                  id="customer-name"
                  className="field flex-1"
                  value={customerName}
                  onChange={(event) => setCustomerName(event.target.value)}
                  maxLength={100}
                  placeholder="e.g. Asha"
                  autoComplete="name"
                  required
                />
                <button type="submit" className="button-primary" disabled={submitting || selectedQueueId === null}>
                  {submitting ? "Joining…" : "Get my token"} <ArrowRight size={17} />
                </button>
              </div>
              {error ? <p className="mt-3 text-sm font-medium text-rose-700" role="alert">{error}</p> : null}
            </form>
          </div>
        </div>
      </section>

      <section className="mt-8 grid items-start gap-6 lg:grid-cols-[0.68fr_1.32fr]">
        {token ? <TicketCard token={token} onCancel={cancelToken} cancelling={submitting} /> : (
          <div className="rounded-[2rem] border border-ink/10 bg-white/80 p-7 shadow-card">
            <span className="grid h-12 w-12 place-items-center rounded-2xl bg-violet/10 text-violet"><ShieldCheck size={23} /></span>
            <h2 className="mt-5 font-display text-2xl font-bold">Your token appears here</h2>
            <p className="mt-3 leading-7 text-ink/55">Join a queue above to see your position, wait estimate, and live status in one place.</p>
            <div className="mt-6 flex items-center gap-2 text-sm font-semibold text-emerald-700"><Radio size={15} /> Live connection ready</div>
          </div>
        )}
        <QueueBoard snapshot={snapshot} connectionState={connectionState} />
      </section>
    </div>
  );
}
