import { useState, type FormEvent } from 'react';
import { errorMessage } from '../services/api';
import type { OrganiserTicket, TicketPayload } from '../types/management';
import Modal from './Modal';

interface TicketFormModalProps {
  /** When provided the form edits this ticket category; otherwise it adds a new one. */
  ticket?: OrganiserTicket;
  onSubmit: (payload: TicketPayload) => Promise<void>;
  onClose: () => void;
}

const inputClass = 'w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-slate-800';

export default function TicketFormModal({ ticket, onSubmit, onClose }: TicketFormModalProps) {
  const [name, setName] = useState(ticket?.name ?? '');
  const [price, setPrice] = useState(ticket ? String(ticket.price) : '');
  const [totalSeats, setTotalSeats] = useState(ticket ? String(ticket.totalSeats) : '');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onSubmit({ name: name.trim(), price: Number(price), totalSeats: Number(totalSeats) });
    } catch (err) {
      setError(errorMessage(err, 'Unable to save the ticket category. Please try again.'));
      setSubmitting(false);
    }
  }

  return (
    <Modal title={ticket ? 'Edit ticket category' : 'Add ticket category'} onClose={onClose}>
      <form className="mt-4 space-y-4" onSubmit={handleSubmit}>
        {error && (
          <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
        )}
        <div>
          <label htmlFor="tk-name" className="mb-1 block text-sm font-medium text-slate-700">Name</label>
          <input id="tk-name" required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} placeholder="General, VIP..." className={inputClass} />
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="tk-price" className="mb-1 block text-sm font-medium text-slate-700">Price (INR)</label>
            <input id="tk-price" type="number" required min={0} step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} className={inputClass} />
          </div>
          <div>
            <label htmlFor="tk-seats" className="mb-1 block text-sm font-medium text-slate-700">Total seats</label>
            <input id="tk-seats" type="number" required min={ticket ? Math.max(1, ticket.bookedSeats) : 1} max={100000} step={1} value={totalSeats} onChange={(e) => setTotalSeats(e.target.value)} className={inputClass} />
            {ticket && ticket.bookedSeats > 0 && (
              <p className="mt-1 text-xs text-slate-500">{ticket.bookedSeats} already booked.</p>
            )}
          </div>
        </div>
        <div className="flex gap-3 pt-2">
          <button type="submit" className="btn-primary flex-1 disabled:opacity-60" disabled={submitting}>
            {submitting ? 'Saving...' : ticket ? 'Save changes' : 'Add category'}
          </button>
          <button type="button" className="btn-secondary flex-1" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}
