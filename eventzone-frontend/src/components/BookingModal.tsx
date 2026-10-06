import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError, createBooking } from '../services/api';
import type { Booking } from '../types/booking';
import type { EventDetail } from '../types/event';
import { formatDateTime, formatPrice } from '../utils/format';

const MAX_QUANTITY = 5;

interface BookingModalProps {
  event: EventDetail;
  onClose: () => void;
  /** Called after any attempt that may have changed seat availability so the page can refresh. */
  onSeatsChanged: () => void;
}

export default function BookingModal({ event, onClose, onSeatsChanged }: BookingModalProps) {
  const { token, logout } = useAuth();
  const available = useMemo(() => event.ticketCategories.filter((t) => t.availableSeats > 0), [event]);

  const [ticketId, setTicketId] = useState(available[0]?.id ?? '');
  const [quantity, setQuantity] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [booking, setBooking] = useState<Booking | null>(null);

  const ticket = available.find((t) => t.id === ticketId);
  const maxQuantity = Math.min(MAX_QUANTITY, ticket?.availableSeats ?? 1);
  const total = ticket ? ticket.price * quantity : 0;

  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') {
        onClose();
      }
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  function selectTicket(id: string) {
    setTicketId(id);
    const next = available.find((t) => t.id === id);
    setQuantity((q) => Math.min(q, Math.min(MAX_QUANTITY, next?.availableSeats ?? 1)));
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!token || !ticket) {
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      setBooking(await createBooking(token, ticket.id, quantity));
      onSeatsChanged();
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        logout();
        onClose();
        return;
      }
      setError(err instanceof Error ? err.message : 'Unable to complete the booking. Please try again.');
      onSeatsChanged();
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 px-4" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="booking-title"
        className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        {booking ? (
          <div>
            <h2 id="booking-title" className="text-xl font-semibold text-slate-900">Booking confirmed</h2>
            <p className="mt-2 text-sm text-slate-600">Keep your booking reference for your records.</p>
            <p className="mt-4 rounded-xl bg-brand-50 px-4 py-3 text-center text-2xl font-bold tracking-wider text-brand-700">
              {booking.bookingRef}
            </p>
            <dl className="mt-4 space-y-1 text-sm text-slate-700">
              <div className="flex justify-between"><dt>Event</dt><dd className="font-medium">{booking.eventTitle}</dd></div>
              <div className="flex justify-between"><dt>When</dt><dd>{formatDateTime(booking.eventDate)}</dd></div>
              <div className="flex justify-between"><dt>Tickets</dt><dd>{booking.quantity} x {booking.ticketCategory}</dd></div>
              <div className="flex justify-between"><dt>Total</dt><dd className="font-semibold">{formatPrice(booking.totalAmount)}</dd></div>
            </dl>
            <div className="mt-6 flex gap-3">
              <Link to="/bookings" className="btn-primary flex-1">View my bookings</Link>
              <button type="button" className="btn-secondary flex-1" onClick={onClose}>Close</button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <h2 id="booking-title" className="text-xl font-semibold text-slate-900">Book tickets</h2>
            <p className="mt-1 text-sm text-slate-600">{event.title}</p>

            {error && (
              <div role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                {error}
              </div>
            )}

            {available.length === 0 ? (
              <p className="mt-4 text-sm text-slate-600">All ticket categories are sold out.</p>
            ) : (
              <div className="mt-4 space-y-4">
                <div>
                  <label htmlFor="ticket" className="mb-1 block text-sm font-medium text-slate-700">Ticket category</label>
                  <select id="ticket" value={ticketId} onChange={(e) => selectTicket(e.target.value)} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5">
                    {available.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name} - {formatPrice(t.price)} ({t.availableSeats} left)
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="quantity" className="mb-1 block text-sm font-medium text-slate-700">Quantity</label>
                  <select id="quantity" value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5">
                    {Array.from({ length: maxQuantity }, (_, i) => i + 1).map((n) => (
                      <option key={n} value={n}>{n}</option>
                    ))}
                  </select>
                </div>
                <p className="flex justify-between text-sm text-slate-700">
                  <span>Total</span>
                  <span className="text-lg font-bold text-slate-900">{formatPrice(total)}</span>
                </p>
              </div>
            )}

            <div className="mt-6 flex gap-3">
              <button type="submit" className="btn-primary flex-1 disabled:opacity-60" disabled={submitting || !ticket}>
                {submitting ? 'Booking...' : 'Confirm booking'}
              </button>
              <button type="button" className="btn-secondary flex-1" onClick={onClose}>Cancel</button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
