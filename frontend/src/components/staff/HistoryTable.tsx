import { StatusBadge } from "../StatusBadge";
import type { HistoryPage } from "../../types";

interface HistoryTableProps {
  history: HistoryPage | null;
  onPrevious: () => void;
  onNext: () => void;
}

export function HistoryTable({ history, onPrevious, onNext }: HistoryTableProps) {
  return (
    <section className="overflow-hidden rounded-[2rem] bg-white shadow-card" aria-labelledby="history-title">
      <div className="flex items-center justify-between gap-4 border-b border-ink/10 p-6 lg:p-8">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.2em] text-ink/40">Past tokens</p>
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
        <button type="button" className="button-secondary py-2" disabled={!history || history.page === 0} onClick={onPrevious}>Previous</button>
        <span className="text-sm text-ink/50">Page {(history?.page ?? 0) + 1} of {Math.max(history?.totalPages ?? 1, 1)}</span>
        <button type="button" className="button-secondary py-2" disabled={!history || history.page + 1 >= history.totalPages} onClick={onNext}>Next</button>
      </div>
    </section>
  );
}
