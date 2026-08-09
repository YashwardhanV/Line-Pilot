import { Clock3, MapPin, Ticket } from "lucide-react";
import type { TokenResponse } from "../types";
import { StatusBadge } from "./StatusBadge";

interface TicketCardProps {
  token: TokenResponse;
  onCancel: () => void;
  cancelling: boolean;
}

export function TicketCard({ token, onCancel, cancelling }: TicketCardProps) {
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
