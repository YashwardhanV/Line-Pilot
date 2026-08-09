import { Clock3, Radio, UsersRound } from "lucide-react";
import type { QueueSnapshot } from "../types";
import { StatusBadge } from "./StatusBadge";

interface QueueBoardProps {
  snapshot: QueueSnapshot | null;
  connectionState: "idle" | "live" | "reconnecting";
}

export function QueueBoard({ snapshot, connectionState }: QueueBoardProps) {
  if (!snapshot) {
    return <div className="h-80 animate-pulse rounded-[2rem] border border-white bg-white/55 shadow-card" aria-label="Loading live queue" />;
  }

  const active = snapshot.tokens.filter((token) => token.status === "CALLED" || token.status === "SERVING");
  const waiting = snapshot.tokens.filter((token) => token.status === "WAITING").slice(0, 8);

  return (
    <section className="overflow-hidden rounded-[2rem] border border-white bg-white/90 shadow-card" aria-labelledby="live-board-title">
      <div className="relative flex flex-col gap-5 overflow-hidden bg-ink p-6 text-white sm:flex-row sm:items-start sm:justify-between lg:p-8">
        <div className="absolute -right-12 -top-20 h-52 w-52 rounded-full bg-violet/50 blur-3xl" />
        <div className="relative">
          <div className="mb-3 flex items-center gap-2 text-xs font-bold uppercase tracking-[0.2em] text-aqua">
            <Radio size={15} aria-hidden="true" />
            <span>{connectionState === "live" ? "Live board" : "Reconnecting"}</span>
          </div>
          <h2 id="live-board-title" className="font-display text-3xl font-bold">{snapshot.queueName}</h2>
          <p className="mt-1 text-white/60">{snapshot.location}</p>
        </div>
        <span className={`relative w-fit rounded-full px-3 py-1 text-sm font-semibold ${snapshot.open ? "bg-aqua text-ink" : "bg-white/10 text-white/70"}`}>
          {snapshot.open ? "Open for joining" : "Closed to new joins"}
        </span>
      </div>

      <div className="grid border-b border-ink/10 sm:grid-cols-2 sm:divide-x sm:divide-ink/10">
        <div className="p-6 lg:p-8">
          <p className="flex items-center gap-2 text-sm text-ink/50"><UsersRound size={17} /> Waiting</p>
          <p className="mt-2 font-display text-5xl font-bold">{snapshot.waitingCount}</p>
        </div>
        <div className="p-6 lg:p-8">
          <p className="flex items-center gap-2 text-sm text-ink/50"><Clock3 size={17} /> Estimated queue time</p>
          <p className="mt-2 font-display text-5xl font-bold">{snapshot.estimatedWaitMinutes}<span className="ml-2 text-xl text-ink/35">min</span></p>
        </div>
      </div>

      <div className="grid gap-8 p-6 lg:grid-cols-[0.8fr_1.2fr] lg:p-8">
        <div>
          <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-ink/40">Now serving</h3>
          <div className="mt-4 space-y-3">
            {active.length === 0 ? (
              <p className="rounded-2xl border border-dashed border-ink/15 p-5 text-sm text-ink/45">No token is being served yet.</p>
            ) : active.map((token) => (
              <div key={token.id} className="rounded-2xl bg-violet/5 p-5 text-ink ring-1 ring-violet/10">
                <p className="font-display text-3xl font-bold">{token.displayNumber}</p>
                <div className="mt-3 flex items-center justify-between gap-3">
                  <StatusBadge status={token.status} />
                  <span className="text-xs text-ink/55">{token.claimedBy}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div>
          <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-ink/40">Coming up</h3>
          <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-2 xl:grid-cols-4">
            {waiting.length === 0 ? (
              <p className="col-span-full rounded-2xl border border-dashed border-ink/15 p-5 text-sm text-ink/45">The waiting line is clear.</p>
            ) : waiting.map((token) => (
              <div key={token.id} className="rounded-2xl border border-ink/10 bg-canvas px-4 py-5 text-center">
                <span className="font-display text-xl font-bold">{token.displayNumber}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
