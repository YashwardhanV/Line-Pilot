import type { FormEvent } from "react";
import { UserRoundCheck } from "lucide-react";

interface StaffLoginProps {
  username: string;
  password: string;
  onUsernameChange: (value: string) => void;
  onPasswordChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  busy: boolean;
  error: string | null;
}

export function StaffLogin({ username, password, onUsernameChange, onPasswordChange, onSubmit, busy, error }: StaffLoginProps) {
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
      <form onSubmit={onSubmit} className="flex flex-col justify-center rounded-[2.5rem] border border-white bg-white/90 p-7 shadow-card sm:p-10">
        <span className="grid h-12 w-12 place-items-center rounded-2xl bg-violet/10 text-violet"><UserRoundCheck size={22} /></span>
        <p className="mt-7 text-xs font-bold uppercase tracking-[0.2em] text-violet">Staff sign-in</p>
        <h2 className="mt-2 font-display text-3xl font-bold">Open staff console</h2>
        <div className="mt-6 space-y-4">
          <div>
            <label htmlFor="staff-username" className="text-sm font-semibold">Username</label>
            <input id="staff-username" className="field mt-2" value={username} onChange={(event) => onUsernameChange(event.target.value)} autoComplete="username" required />
          </div>
          <div>
            <label htmlFor="staff-password" className="text-sm font-semibold">Password</label>
            <input id="staff-password" className="field mt-2" type="password" value={password} onChange={(event) => onPasswordChange(event.target.value)} autoComplete="current-password" required />
          </div>
        </div>
        {error ? <p className="mt-4 text-sm font-medium text-rose-700" role="alert">{error}</p> : null}
        <button type="submit" className="button-primary mt-6 w-full" disabled={busy}>{busy ? "Signing in…" : "Open dashboard"}</button>
      </form>
    </div>
  );
}
