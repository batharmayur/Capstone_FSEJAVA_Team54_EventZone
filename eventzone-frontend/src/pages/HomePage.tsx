import { Link } from 'react-router-dom';

const features = [
  { title: 'Event Tracking', text: 'Monitor approvals, timelines, logistics, and delivery checkpoints from one dashboard.' },
  { title: 'Stakeholder Reviews', text: 'Coordinate organisers, faculty, vendors, and admins with a clear approval workflow.' },
  { title: 'Secure Access', text: 'Support attendee, organiser, and admin journeys with role-based experiences.' }
];

export default function HomePage() {
  return (
    <main className="min-h-screen">
      <header className="mx-auto flex max-w-7xl items-center justify-between px-6 py-6">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 font-bold text-white">E</div>
          <div>
            <p className="text-lg font-bold text-slate-900">EventZone</p>
          </div>
        </div>

        <nav className="hidden items-center gap-8 text-sm font-medium text-slate-600 md:flex">
          <a href="#features">Features</a>
          <a href="#workflow">Workflow</a>
          <a href="#about">About</a>
        </nav>

        <div className="flex items-center gap-3">
          <Link to="/login" className="btn-secondary">Login</Link>
          <Link to="/dashboard" className="btn-primary">Open Dashboard</Link>
        </div>
      </header>

      <section className="mx-auto grid max-w-7xl items-center gap-12 px-6 pb-20 pt-12 md:grid-cols-2">
        <div>
          <span className="mb-4 inline-flex rounded-full border border-brand-200 bg-brand-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.12em] text-brand-700">
            Event management system
          </span>
          <h1 className="max-w-xl text-4xl font-black tracking-tight text-slate-900 md:text-6xl">
            Plan, review, and launch events with less operational drag.
          </h1>
          <p className="mt-6 max-w-lg text-lg text-slate-600">
            EventZone is a full-stack event operations platform built for attendees, organisers, and admins to coordinate schedules, approvals, and execution from one workspace.
          </p>
          <div className="mt-8 flex flex-wrap gap-4">
            <Link to="/dashboard" className="btn-primary">View Dashboard</Link>
            <Link to="/login" className="btn-secondary">Sign In</Link>
          </div>
          <div className="mt-10 flex gap-10 text-sm text-slate-600">
            <div>
              <p className="text-2xl font-bold text-slate-900">320+</p>
              <p>Events tracked</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-900">96%</p>
              <p>On-time execution</p>
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-900">24/7</p>
              <p>Status visibility</p>
            </div>
          </div>
        </div>

        <div className="grid-pattern card-surface relative overflow-hidden p-6">
          <div className="rounded-2xl bg-slate-950 p-6 text-white shadow-2xl">
            <div className="flex items-center justify-between">
              <p className="text-sm text-slate-300">Operations Overview</p>
              <span className="rounded-full bg-emerald-500/20 px-2 py-1 text-xs text-emerald-300">Live</span>
            </div>
            <div className="mt-6 space-y-4">
              <div className="rounded-xl bg-slate-900 p-4">
                <div className="mb-2 flex items-center justify-between text-sm text-slate-300">
                  <span>Rock Night 2025</span>
                  <span className="text-emerald-400">72%</span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-slate-700">
                  <div className="h-full w-[72%] rounded-full bg-gradient-to-r from-brand-400 to-emerald-400" />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="rounded-xl bg-slate-900 p-4">
                  <p className="text-sm text-slate-400">Organisers</p>
                  <p className="mt-2 text-2xl font-bold">12</p>
                </div>
                <div className="rounded-xl bg-slate-900 p-4">
                  <p className="text-sm text-slate-400">Approved</p>
                  <p className="mt-2 text-2xl font-bold">86%</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="features" className="mx-auto max-w-7xl px-6 py-16">
        <div className="mb-10 text-center">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-brand-700">Why teams choose it</p>
          <h2 className="mt-3 text-3xl font-bold text-slate-900">Built for real event operations</h2>
        </div>
        <div className="grid gap-6 md:grid-cols-3">
          {features.map((feature) => (
            <div key={feature.title} className="card-surface p-6">
              <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-brand-50 font-bold text-brand-700">✓</div>
              <h3 className="mb-2 text-xl font-semibold text-slate-900">{feature.title}</h3>
              <p className="text-slate-600">{feature.text}</p>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}