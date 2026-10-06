import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import BookingModal from '../components/BookingModal';
import CoverImage from '../components/CoverImage';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import { ApiError, fetchEvent } from '../services/api';
import type { EventDetail } from '../types/event';
import { formatDateTime, formatPrice } from '../utils/format';

export default function EventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const location = useLocation();
  const [event, setEvent] = useState<EventDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [bookingOpen, setBookingOpen] = useState(false);

  useEffect(() => {
    if (!id) {
      return;
    }

    let active = true;
    setLoading(true);
    setError(null);

    fetchEvent(id)
      .then((data) => {
        if (active) {
          setEvent(data);
        }
      })
      .catch((err: unknown) => {
        if (active) {
          setEvent(null);
          setError(
            err instanceof ApiError && (err.status === 404 || err.status === 400)
              ? 'This event could not be found.'
              : 'Unable to load event. Please make sure the backend is running and try again.'
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [id, reloadKey]);

  const hasEnded = event ? new Date(event.eventDate).getTime() <= Date.now() : false;
  const soldOut = event ? event.ticketCategories.every((t) => t.availableSeats === 0) : false;

  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-5xl px-6 py-10">
        <Link to="/" className="mb-6 inline-block text-sm font-medium text-brand-700">&larr; Back to events</Link>

        {loading && <p className="text-slate-500">Loading event...</p>}

        {error && (
          <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {event && (
          <article className="card-surface overflow-hidden">
            <CoverImage src={event.coverImageUrl} alt={event.title} className="h-72 w-full object-cover" />
            <div className="p-6 md:p-8">
              <span className="rounded-full bg-brand-50 px-2.5 py-1 text-xs font-medium text-brand-700">{event.category}</span>
              <h1 className="mt-3 text-3xl font-bold text-slate-900">{event.title}</h1>
              <p className="mt-2 text-slate-600">{formatDateTime(event.eventDate)}</p>
              <p className="text-slate-600">{event.venue}</p>

              {event.description && <p className="mt-6 whitespace-pre-line text-slate-700">{event.description}</p>}

              <h2 className="mt-8 text-xl font-semibold text-slate-900">Tickets</h2>
              {event.ticketCategories.length === 0 ? (
                <p className="mt-3 text-slate-500">Tickets are not available yet.</p>
              ) : (
                <div className="mt-3 overflow-x-auto rounded-xl border border-slate-200">
                  <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                    <thead className="bg-slate-50 text-slate-500">
                      <tr>
                        <th className="px-4 py-3 font-medium">Category</th>
                        <th className="px-4 py-3 font-medium">Price</th>
                        <th className="px-4 py-3 font-medium">Availability</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200 bg-white text-slate-700">
                      {event.ticketCategories.map((ticket) => (
                        <tr key={ticket.id}>
                          <td className="px-4 py-3 font-medium text-slate-900">{ticket.name}</td>
                          <td className="px-4 py-3">{formatPrice(ticket.price)}</td>
                          <td className="px-4 py-3">
                            {ticket.availableSeats === 0 ? (
                              <span className="font-medium text-red-600">Sold out</span>
                            ) : (
                              `${ticket.availableSeats} of ${ticket.totalSeats} seats available`
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              {event.ticketCategories.length > 0 && (
                <div className="mt-6">
                  {hasEnded ? (
                    <p className="text-sm font-medium text-slate-500">This event has ended.</p>
                  ) : soldOut ? (
                    <p className="text-sm font-medium text-red-600">This event is sold out.</p>
                  ) : user ? (
                    <button type="button" className="btn-primary" onClick={() => setBookingOpen(true)}>Book tickets</button>
                  ) : (
                    <Link to="/login" state={{ from: location }} className="btn-primary">Log in to book tickets</Link>
                  )}
                </div>
              )}
            </div>
          </article>
        )}

        {event && bookingOpen && (
          <BookingModal
            event={event}
            onClose={() => setBookingOpen(false)}
            onSeatsChanged={() => setReloadKey((k) => k + 1)}
          />
        )}
      </main>
    </div>
  );
}
