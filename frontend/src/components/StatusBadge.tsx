import type { TokenStatus } from "../types";

const statusStyles: Record<TokenStatus, string> = {
  WAITING: "bg-amber-100 text-amber-900",
  CALLED: "bg-sky-100 text-sky-900",
  SERVING: "bg-emerald-100 text-emerald-900",
  COMPLETED: "bg-slate-200 text-slate-700",
  SKIPPED: "bg-rose-100 text-rose-900",
  CANCELLED: "bg-stone-200 text-stone-700",
};

export function StatusBadge({ status }: { status: TokenStatus }) {
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold tracking-wide ${statusStyles[status]}`}>
      {status.replace("_", " ")}
    </span>
  );
}
