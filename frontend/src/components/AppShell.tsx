import { Gauge, Radio, Route } from "lucide-react";
import type { ReactNode } from "react";

function navClass(isActive: boolean) {
  return `rounded-xl px-4 py-2 text-sm font-semibold transition ${
    isActive ? "bg-violet text-white shadow-sm" : "text-ink/60 hover:bg-ink/5 hover:text-ink"
  }`;
}

export function AppShell({ children }: { children: ReactNode }) {
  const staffPage = window.location.pathname.startsWith("/staff");

  return (
    <div className="min-h-screen bg-canvas text-ink">
      <header className="sticky top-0 z-30 border-b border-white/70 bg-canvas/80 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-5 py-3 lg:px-8">
          <a href="/" className="flex items-center gap-3" aria-label="LinePilot customer home">
            <span className="grid h-11 w-11 place-items-center rounded-2xl bg-ink text-aqua shadow-sm">
              <Route size={22} aria-hidden="true" />
            </span>
            <span>
              <span className="block font-display text-xl font-bold leading-none tracking-tight">LinePilot</span>
              <span className="mt-1 hidden text-[10px] font-bold uppercase tracking-[0.18em] text-ink/40 sm:block">
                Live service queues
              </span>
            </span>
          </a>

          <div className="flex items-center gap-3">
            <span className="hidden items-center gap-2 rounded-full bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-700 md:inline-flex">
              <Radio size={13} aria-hidden="true" /> Live
            </span>
            <nav className="flex items-center gap-1 rounded-2xl border border-ink/10 bg-white/80 p-1" aria-label="Primary navigation">
              <a href="/" className={navClass(!staffPage)}>Join queue</a>
              <a href="/staff" className={navClass(staffPage)}>
                <span className="inline-flex items-center gap-2">
                  <Gauge size={16} aria-hidden="true" /> Staff console
                </span>
              </a>
            </nav>
          </div>
        </div>
      </header>

      <main>{children}</main>

      <footer className="mx-auto flex max-w-7xl flex-col gap-2 border-t border-ink/10 px-5 py-8 text-sm text-ink/55 sm:flex-row sm:items-center sm:justify-between lg:px-8">
        <p><span className="font-semibold text-ink">LinePilot</span> by Yashwardhan Verma.</p>
        <p>Fair ordering · Transaction-safe claims · Live SSE updates</p>
      </footer>
    </div>
  );
}
