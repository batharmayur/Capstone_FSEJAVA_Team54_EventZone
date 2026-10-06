import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import { ApiError, cancelBooking, fetchMyBookings } from '../services/api';
import type { Booking } from '../types/booking';
import { formatDateTime, formatPrice } from '../utils/format';

export default function MyBookingsPage() {
  const { token, logout } = useAuth();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  const handleError = useCallback(
    (err: unknown, fallback: string) => {
      if (err instanceof ApiError && err.status === 401) {
        logout();
        return;
      }
      setError(err instanceof ApiError ? err.message : fallback);
    },
    [logout]
  );

  useEffect(() => {
    if (!token) {
      return;
    }
    let active = true;
    fetchMyBookings(token)
      .then((items) => {
        if (active) {
          setBookings(items);
        }
      })
      .catch((err: unknown) => {
        if (active) {
          handleError(err, 'Unable to load your bookings. Please make sure the backend is running and try again.');
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
  }, [token, handleError]);

  async function handleCancel(booking: Booking) {
    if (!token || !window.confirm(`Cancel booking ${booking.bookingRef}? Your seats will be released.`)) {
      return;
    }
    setError(null);
    setCancellingId(booking.id);
    try {
      const updated = await cancelBooking(token, booking.id);
      setBookings((current) => current.map((b) => (b.id === updated.id ? updated : b)));
    } catch (err) {
      handleError(err, 'Unable to cancel this booking. Please try again.');
    } finally {
      setCancellingId(null);
    }
  }

  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-6xl px-6 py-10">
        <h1 className="text-3xl font-bold text-slate-900">My bookings</h1>

        {error && (
          <div role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {loading ? (
          <p className="mt-6 text-slate-500">Loading your bookings...</p>
        ) : bookings.length === 0 && !error ? (
          <div className="card-surface mt-6 p-8 text-center">
            <p className="text-slate-600">You have no bookings yet.</p>
            <Link to="/" className="btn-primary mt-4">Browse events</Link>
          </div>
        ) : (
          <div className="card-surface mt-6 overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
              <thead className="bg-slate-50 text-slate-500">
                <tr>
                  <th className="px-4 py-3 font-medium">Event</th>
                  <th className="px-4 py-3 font-medium">Category</th>
                  <th className="px-4 py-3 font-medium">Qty</th>
                  <th className="px-4 py-3 font-medium">Total</th>
                  <th className="px-4 py-3 font-medium">Booking ref</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 font-medium"><span className="sr-only">Actions</span></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white text-slate-700">
                {bookings.map((booking) => (
                  <tr key={booking.id}>
                    <td className="px-4 py-3">
                      <Link to={`/events/${booking.eventId}`} className="font-medium text-slate-900 hover:text-brand-700">
                        {booking.eventTitle}
                      </Link>
                      <p className="text-xs text-slate-500">{formatDateTime(booking.eventDate)} · {booking.venue}</p>
                    </td>
                    <td className="px-4 py-3">{booking.ticketCategory}</td>
                    <td className="px-4 py-3">{booking.quantity}</td>
                    <td className="px-4 py-3">{formatPrice(booking.totalAmount)}</td>
                    <td className="px-4 py-3 font-mono text-xs">{booking.bookingRef}</td>
                    <td className="px-4 py-3">
                      <span
                        className={
                          booking.status === 'CONFIRMED'
                            ? 'rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700'
                            : 'rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600'
                        }
                      >
                        {booking.status === 'CONFIRMED' ? 'Confirmed' : 'Cancelled'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      {booking.status === 'CONFIRMED' && (
                        <button
                          type="button"
                          className="btn-secondary disabled:opacity-60"
                          disabled={cancellingId === booking.id}
                          onClick={() => handleCancel(booking)}
                        >
                          {cancellingId === booking.id ? 'Cancelling...' : 'Cancel'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
