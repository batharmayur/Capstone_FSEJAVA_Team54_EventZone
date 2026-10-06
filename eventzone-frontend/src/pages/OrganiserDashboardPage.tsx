import { useCallback, useEffect, useState } from 'react';
import EventFormModal from '../components/EventFormModal';
import Navbar from '../components/Navbar';
import TicketFormModal from '../components/TicketFormModal';
import { useAuth } from '../context/AuthContext';
import * as api from '../services/api';
import type { EventCategory } from '../types/event';
import type { EventPayload, OrganiserEvent, OrganiserTicket, TicketPayload } from '../types/management';
import { formatDateTime, formatPrice } from '../utils/format';

type ModalState =
  | { kind: 'event'; event?: OrganiserEvent }
  | { kind: 'ticket'; eventId: string; ticket?: OrganiserTicket }
  | null;

export default function OrganiserDashboardPage() {
  const { token, handleSessionError } = useAuth();
  const [events, setEvents] = useState<OrganiserEvent[]>([]);
  const [categories, setCategories] = useState<EventCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [modal, setModal] = useState<ModalState>(null);

  const load = useCallback(async () => {
    if (!token) {
      return;
    }
    try {
      const [mine, cats] = await Promise.all([api.fetchOrganiserEvents(token), api.fetchCategories()]);
      setEvents(mine);
      setCategories(cats);
      setError(null);
    } catch (err) {
      if (!handleSessionError(err)) {
        setError(api.errorMessage(err, 'Unable to load your events. Please make sure the backend is running.'));
      }
    } finally {
      setLoading(false);
    }
  }, [token, handleSessionError]);

  useEffect(() => {
    void load();
  }, [load]);

  /** Runs a change, refreshes the list, and shows any error on the page. Rethrows so forms can show it too. */
  async function mutate(action: () => Promise<unknown>) {
    if (!token) {
      return;
    }
    try {
      await action();
      await load();
    } catch (err) {
      if (handleSessionError(err)) {
        return;
      }
      throw err;
    }
  }

  async function runAction(action: () => Promise<unknown>, fallback: string) {
    setError(null);
    try {
      await mutate(action);
    } catch (err) {
      setError(api.errorMessage(err, fallback));
    }
  }

  async function saveEvent(existing: OrganiserEvent | undefined, payload: EventPayload) {
    await mutate(() => (existing ? api.updateEvent(token!, existing.id, payload) : api.createEvent(token!, payload)));
    setModal(null);
  }

  async function saveTicket(eventId: string, existing: OrganiserTicket | undefined, payload: TicketPayload) {
    await mutate(() =>
      existing ? api.updateTicketCategory(token!, existing.id, payload) : api.addTicketCategory(token!, eventId, payload)
    );
    setModal(null);
  }

  function removeEvent(event: OrganiserEvent) {
    if (window.confirm(`Delete "${event.title}"? This cannot be undone.`)) {
      void runAction(() => api.deleteEvent(token!, event.id), 'Unable to delete this event.');
    }
  }

  function removeTicket(ticket: OrganiserTicket) {
    if (window.confirm(`Delete the "${ticket.name}" ticket category?`)) {
      void runAction(() => api.deleteTicketCategory(token!, ticket.id), 'Unable to delete this ticket category.');
    }
  }

  const modalEvent = modal?.kind === 'event' ? modal : null;
  const modalTicket = modal?.kind === 'ticket' ? modal : null;

  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-6xl px-6 py-10">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-slate-900">My events</h1>
            <p className="mt-1 text-slate-600">Create events, set ticket categories, and track bookings.</p>
          </div>
          <button type="button" className="btn-primary" onClick={() => setModal({ kind: 'event' })}>Add event</button>
        </div>

        {error && (
          <div role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
        )}

        {loading ? (
          <p className="mt-6 text-slate-500">Loading your events...</p>
        ) : events.length === 0 && !error ? (
          <div className="card-surface mt-6 p-8 text-center text-slate-600">You have not created any events yet.</div>
        ) : (
          <div className="mt-6 space-y-6">
            {events.map((event) => (
              <section key={event.id} className="card-surface overflow-hidden" aria-label={event.title}>
                <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-200 p-5">
                  <div>
                    <div className="flex items-center gap-2">
                      <h2 className="text-lg font-semibold text-slate-900">{event.title}</h2>
                      <span
                        className={
                          event.active
                            ? 'rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700'
                            : 'rounded-full bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-700'
                        }
                      >
                        {event.active ? 'Active' : 'Deactivated by admin'}
                      </span>
                    </div>
                    <p className="mt-1 text-sm text-slate-600">
                      {event.category} · {formatDateTime(event.eventDate)} · {event.venue}
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <button type="button" className="btn-secondary" onClick={() => setModal({ kind: 'ticket', eventId: event.id })}>Add ticket category</button>
                    <button type="button" className="btn-secondary" onClick={() => setModal({ kind: 'event', event })}>Edit</button>
                    <button type="button" className="btn-secondary" onClick={() => removeEvent(event)}>Delete</button>
                  </div>
                </div>

                {event.ticketCategories.length === 0 ? (
                  <p className="p-5 text-sm text-slate-500">No ticket categories yet. Add one so attendees can book.</p>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                      <thead className="bg-slate-50 text-slate-500">
                        <tr>
                          <th className="px-5 py-3 font-medium">Category</th>
                          <th className="px-5 py-3 font-medium">Price</th>
                          <th className="px-5 py-3 font-medium">Seats (available / total)</th>
                          <th className="px-5 py-3 font-medium">Seats booked</th>
                          <th className="px-5 py-3 font-medium">Bookings</th>
                          <th className="px-5 py-3 font-medium"><span className="sr-only">Actions</span></th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-200 bg-white text-slate-700">
                        {event.ticketCategories.map((ticket) => (
                          <tr key={ticket.id}>
                            <td className="px-5 py-3 font-medium text-slate-900">{ticket.name}</td>
                            <td className="px-5 py-3">{formatPrice(ticket.price)}</td>
                            <td className="px-5 py-3">{ticket.availableSeats} / {ticket.totalSeats}</td>
                            <td className="px-5 py-3">{ticket.bookedSeats}</td>
                            <td className="px-5 py-3">{ticket.bookingCount}</td>
                            <td className="px-5 py-3 text-right">
                              <div className="flex justify-end gap-2">
                                <button type="button" className="btn-secondary" onClick={() => setModal({ kind: 'ticket', eventId: event.id, ticket })}>Edit</button>
                                <button type="button" className="btn-secondary" onClick={() => removeTicket(ticket)}>Delete</button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            ))}
          </div>
        )}
      </main>

      {modalEvent && (
        <EventFormModal
          categories={categories}
          event={modalEvent.event}
          onSubmit={(payload) => saveEvent(modalEvent.event, payload)}
          onClose={() => setModal(null)}
        />
      )}
      {modalTicket && (
        <TicketFormModal
          ticket={modalTicket.ticket}
          onSubmit={(payload) => saveTicket(modalTicket.eventId, modalTicket.ticket, payload)}
          onClose={() => setModal(null)}
        />
      )}
    </div>
  );
}
