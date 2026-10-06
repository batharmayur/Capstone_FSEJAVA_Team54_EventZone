import { useState, type FormEvent } from 'react';
import { errorMessage } from '../services/api';
import type { EventCategory } from '../types/event';
import type { EventPayload, OrganiserEvent } from '../types/management';
import Modal from './Modal';

interface EventFormModalProps {
  categories: EventCategory[];
  /** When provided the form edits this event; otherwise it creates a new one. */
  event?: OrganiserEvent;
  onSubmit: (payload: EventPayload) => Promise<void>;
  onClose: () => void;
}

const inputClass = 'w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-slate-800';

function nowForInput() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return now.toISOString().slice(0, 16);
}

export default function EventFormModal({ categories, event, onSubmit, onClose }: EventFormModalProps) {
  const [title, setTitle] = useState(event?.title ?? '');
  const [description, setDescription] = useState(event?.description ?? '');
  const [eventDate, setEventDate] = useState(event ? event.eventDate.slice(0, 16) : '');
  const [venue, setVenue] = useState(event?.venue ?? '');
  const [coverImageUrl, setCoverImageUrl] = useState(event?.coverImageUrl ?? '');
  const [categoryId, setCategoryId] = useState(event?.categoryId ?? categories[0]?.id ?? '');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onSubmit({
        title: title.trim(),
        description: description.trim() || null,
        eventDate: `${eventDate}:00`,
        venue: venue.trim(),
        coverImageUrl: coverImageUrl.trim() || null,
        categoryId
      });
    } catch (err) {
      setError(errorMessage(err, 'Unable to save the event. Please try again.'));
      setSubmitting(false);
    }
  }

  return (
    <Modal title={event ? 'Edit event' : 'Add event'} onClose={onClose}>
      <form className="mt-4 space-y-4" onSubmit={handleSubmit}>
        {error && (
          <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
        )}
        <div>
          <label htmlFor="ev-title" className="mb-1 block text-sm font-medium text-slate-700">Title</label>
          <input id="ev-title" required maxLength={200} value={title} onChange={(e) => setTitle(e.target.value)} className={inputClass} />
        </div>
        <div>
          <label htmlFor="ev-category" className="mb-1 block text-sm font-medium text-slate-700">Category</label>
          <select id="ev-category" required value={categoryId} onChange={(e) => setCategoryId(e.target.value)} className={inputClass}>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="ev-date" className="mb-1 block text-sm font-medium text-slate-700">Date and time</label>
            <input id="ev-date" type="datetime-local" required min={nowForInput()} value={eventDate} onChange={(e) => setEventDate(e.target.value)} className={inputClass} />
          </div>
          <div>
            <label htmlFor="ev-venue" className="mb-1 block text-sm font-medium text-slate-700">Venue</label>
            <input id="ev-venue" required maxLength={200} value={venue} onChange={(e) => setVenue(e.target.value)} className={inputClass} />
          </div>
        </div>
        <div>
          <label htmlFor="ev-description" className="mb-1 block text-sm font-medium text-slate-700">Description</label>
          <textarea id="ev-description" rows={3} maxLength={2000} value={description} onChange={(e) => setDescription(e.target.value)} className={inputClass} />
        </div>
        <div>
          <label htmlFor="ev-image" className="mb-1 block text-sm font-medium text-slate-700">Cover image URL (optional)</label>
          <input id="ev-image" value={coverImageUrl} onChange={(e) => setCoverImageUrl(e.target.value)} placeholder="https://... or /images/events/..." className={inputClass} />
        </div>
        <div className="flex gap-3 pt-2">
          <button type="submit" className="btn-primary flex-1 disabled:opacity-60" disabled={submitting || !categoryId}>
            {submitting ? 'Saving...' : event ? 'Save changes' : 'Create event'}
          </button>
          <button type="button" className="btn-secondary flex-1" onClick={onClose}>Cancel</button>
        </div>
      </form>
    </Modal>
  );
}
