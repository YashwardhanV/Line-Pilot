import { CheckCircle2, Play, SkipForward } from "lucide-react";
import { StatusBadge } from "../StatusBadge";
import type { QueueTokenView } from "../../types";

interface OperatorPanelProps {
  assignedToken: QueueTokenView | null;
  staffName: string;
  queueId: number | null;
  busy: boolean;
  onAction: (path: string) => void;
}

export function OperatorPanel({ assignedToken, staffName, queueId, busy, onAction }: OperatorPanelProps) {
  return (
    <section className="rounded-[2rem] bg-ink p-6 text-white shadow-card lg:p-8" aria-labelledby="operator-controls-title">
      <p className="text-xs font-bold uppercase tracking-[0.2em] text-aqua">Operator controls</p>
      <h2 id="operator-controls-title" className="mt-2 font-display text-2xl font-bold">Current assignment</h2>
      {assignedToken ? (
        <div className="mt-6 rounded-3xl bg-white p-6 text-ink">
          <div className="flex items-start justify-between gap-3">
            <p className="font-display text-4xl font-bold">{assignedToken.displayNumber}</p>
            <StatusBadge status={assignedToken.status} />
          </div>
          <p className="mt-3 text-sm text-ink/50">Assigned to {staffName}.</p>
        </div>
      ) : (
        <div className="mt-6 rounded-3xl border border-dashed border-white/20 p-6 text-sm text-white/50">No active token assigned to you.</div>
      )}

      <div className="mt-6 grid gap-3">
        {!assignedToken ? (
          <button type="button" className="button-primary" disabled={busy || queueId === null} onClick={() => onAction(`/queues/${queueId}/call-next`)}>
            <SkipForward size={18} /> Call next
          </button>
        ) : null}
        {assignedToken?.status === "CALLED" ? (
          <>
            <button type="button" className="button-primary" disabled={busy} onClick={() => onAction(`/tokens/${assignedToken.id}/start`)}><Play size={18} /> Start service</button>
            <button type="button" className="button-secondary text-rose-700" disabled={busy} onClick={() => onAction(`/tokens/${assignedToken.id}/skip`)}><SkipForward size={18} /> Mark no-show</button>
          </>
        ) : null}
        {assignedToken?.status === "SERVING" ? (
          <button type="button" className="button-primary" disabled={busy} onClick={() => onAction(`/tokens/${assignedToken.id}/complete`)}><CheckCircle2 size={18} /> Complete service</button>
        ) : null}
      </div>
    </section>
  );
}
