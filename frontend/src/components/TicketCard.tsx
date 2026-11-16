import { Clock3, MapPin, Ticket } from "lucide-react";
import type { TokenResponse } from "../types";
import { StatusBadge } from "./StatusBadge";

interface TicketCardProps {
  token: TokenResponse;
  onCancel: () => void;
  onRejoin: () => void;
  cancelling: boolean;
}

export function TicketCard({ token, onCancel, onRejoin, cancelling }: TicketCardProps) {
  return (
    <section className="relative overflow-hidden rounded-[2rem] bg-violet p-6 text-white shadow-card lg:p-8" aria-labelledby="your-token-title">
      <div className="absolute -right-10 -top-10 h-32 w-32 rounded-full bg-aqua/30 blur-2xl" />
      <div className="relative">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p id="your-token-title" className="text-xs font-bold uppercase tracking-[0.2em] text-white/60">Your live token</p>
            <p className="mt-2 font-display text-5xl font-bold tracking-tight">{token.displayNumber}</p>
          </div>
          <span className="grid h-12 w-12 place-items-center rounded-2xl bg-white/15 text-aqua ring-1 ring-white/20">
            <Ticket size={22} aria-hidden="true" />
          </span>
        </div>
        <div className="mt-5"><StatusBadge status={token.status} /></div>
        {token.status === "CALLED" ? (
          <p className="mt-5 rounded-2xl bg-white px-4 py-3 font-display text-xl font-bold text-ink" role="status">
            It's your turn. Please go to {token.counterLabel ?? "the desk"} now.
          </p>
        ) : null}
        {token.status === "SERVING" && token.counterLabel ? (
          <p className="mt-5 text-sm font-semibold text-white/80">Being served at {token.counterLabel}.</p>
        ) : null}
        <dl className="mt-7 grid gap-4 border-t border-white/20 pt-6 sm:grid-cols-2">
          <div>
            <dt className="flex items-center gap-2 text-sm text-white/60"><MapPin size={15} /> Queue</dt>
            <dd className="mt-1 font-semibold">{token.queueName}</dd>
          </div>
          <div>
            <dt className="flex items-center gap-2 text-sm text-white/60"><Clock3 size={15} /> Estimate</dt>
            <dd className="mt-1 font-semibold">{token.estimatedWaitMinutes} minutes · {token.peopleAhead} ahead</dd>
          </div>
        </dl>
        {token.status === "SKIPPED" ? (
          <div className="mt-7 rounded-2xl bg-white/10 p-4">
            <p className="text-sm text-white/80">You missed your turn. If you are here now, you can go back in the line with a new number.</p>
            <button type="button" onClick={onRejoin} disabled={cancelling} className="button-secondary mt-3 py-2 text-ink">
              {cancelling ? "Rejoining…" : "Put me back in the line"}
            </button>
          </div>
        ) : null}
        {token.status === "WAITING" ? (
          <button
            type="button"
            onClick={onCancel}
            disabled={cancelling}
            className="mt-7 text-sm font-semibold text-white underline decoration-white/40 underline-offset-4 disabled:opacity-50"
          >
            {cancelling ? "Cancelling…" : "Leave this queue"}
          </button>
        ) : null}
      </div>
    </section>
  );
}
