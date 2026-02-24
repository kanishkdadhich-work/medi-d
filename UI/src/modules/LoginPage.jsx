import { useState } from 'react';

export default function LoginPage({ onLogin, loading, error }) {
  const [form, setForm] = useState({ username: '', password: '' });

  return (
    <main className="min-h-screen bg-gradient-to-br from-sky-50 via-white to-emerald-50 px-4 py-12">
      <div className="mx-auto grid w-full max-w-6xl overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-2xl lg:grid-cols-2">
        <section className="relative overflow-hidden bg-med-700 p-10 text-white">
          <div className="absolute -right-10 -top-12 h-44 w-44 rounded-full bg-white/10" />
          <div className="absolute -bottom-14 -left-12 h-52 w-52 rounded-full bg-emerald-300/20" />
          <p className="relative text-xs uppercase tracking-[0.28em] text-med-100">Medical Platform</p>
          <h1 className="relative mt-3 text-6xl font-extrabold leading-none tracking-tight sm:text-7xl">MEDI-D</h1>
          <p className="relative mt-2 text-sm uppercase tracking-[0.18em] text-med-100">Medical & Inventory Management</p>
          <p className="mt-4 text-sm text-med-100">
            Role-based workflow for reception, consultations, pharmacy dispensing, and admin operations.
          </p>
          <ul className="relative mt-8 space-y-3 text-sm text-med-100">
            <li>Reception booking with live slot status</li>
            <li>Doctor consultation and prescription workflow</li>
            <li>Pharmacy inventory and dispensing controls</li>
          </ul>
        </section>

        <section className="p-8 sm:p-10 lg:p-12">
          <h2 className="text-3xl font-semibold text-slate-900">Login</h2>
          <p className="mt-1 text-sm text-slate-500">Enter your staff credentials to continue.</p>

          <form
            className="mt-6 space-y-4"
            onSubmit={(e) => {
              e.preventDefault();
              onLogin(form);
            }}
          >
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Username</label>
              <input
                className="field"
                value={form.username}
                onChange={(e) => setForm((s) => ({ ...s, username: e.target.value }))}
                required
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Password</label>
              <input
                type="password"
                className="field"
                value={form.password}
                onChange={(e) => setForm((s) => ({ ...s, password: e.target.value }))}
                required
              />
            </div>

            {error ? (
              <div className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</div>
            ) : null}

            <button className="btn-primary w-full" disabled={loading}>
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>
        </section>
      </div>
    </main>
  );
}
