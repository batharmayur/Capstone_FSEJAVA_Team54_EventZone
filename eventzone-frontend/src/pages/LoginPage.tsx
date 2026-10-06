import { FormEvent } from 'react';
import { Link } from 'react-router-dom';

export default function LoginPage() {
  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-100 px-6 py-12">
      <div className="grid w-full max-w-5xl overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-soft lg:grid-cols-2">
        <div className="bg-gradient-to-br from-brand-600 to-brand-800 p-10 text-white">
          <p className="mb-6 text-sm uppercase tracking-[0.2em] text-brand-100">Secure portal</p>
          <h1 className="text-4xl font-black">EventZone</h1>
          <p className="mt-6 text-brand-100">
            Manage ticketing, approvals, schedules, and event milestones from one trusted workspace.
          </p>
          <div className="mt-10 rounded-2xl bg-white/10 p-5 backdrop-blur-sm">
            <p className="text-sm text-brand-100">Active events</p>
            <p className="mt-3 text-3xl font-bold">128 active</p>
          </div>
        </div>

        <div className="p-8 md:p-10">
          <div className="mb-8">
            <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-700">Login</p>
            <h2 className="mt-2 text-3xl font-bold text-slate-900">Welcome back</h2>
          </div>

          <form className="space-y-5" onSubmit={handleSubmit}>
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700" htmlFor="email">Email address</label>
              <input
                id="email"
                type="email"
                defaultValue="organiser@eventzone.dev"
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-800 outline-none ring-0 transition focus:border-brand-300 focus:bg-white"
              />
            </div>
            <div>
              <label className="mb-2 block text-sm font-medium text-slate-700" htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                defaultValue="password123"
                className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-slate-800 outline-none ring-0 transition focus:border-brand-300 focus:bg-white"
              />
            </div>
            <div className="flex items-center justify-between text-sm text-slate-500">
              <label className="inline-flex items-center gap-2">
                <input type="checkbox" className="h-4 w-4 rounded border-slate-300" />
                Remember me
              </label>
              <Link to="/" className="font-medium text-brand-700">Forgot password?</Link>
            </div>
            <button type="submit" className="btn-primary w-full">Sign in</button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            Need an account? <Link to="/" className="font-medium text-brand-700">Request access</Link>
          </p>
        </div>
      </div>
    </main>
  );
}