import { Link } from 'react-router-dom';
import CoverImage from './CoverImage';
import { formatDateTime, formatPrice } from '../utils/format';
import type { EventSummary } from '../types/event';

function priceLabel(minPrice: number | null) {
  if (minPrice === null) {
    return 'Tickets coming soon';
  }
  return minPrice === 0 ? 'Free' : `From ${formatPrice(minPrice)}`;
}

export default function EventCard({ event }: { event: EventSummary }) {
  return (
    <Link to={`/events/${event.id}`} className="block focus:outline-none focus:ring-2 focus:ring-brand-300 rounded-2xl">
      <article className="card-surface flex h-full flex-col overflow-hidden transition hover:shadow-lg">
        <CoverImage src={event.coverImageUrl} alt={event.title} className="h-44 w-full object-cover" loading="lazy" />
        <div className="flex flex-1 flex-col gap-2 p-5">
          <span className="w-fit rounded-full bg-brand-50 px-2.5 py-1 text-xs font-medium text-brand-700">{event.category}</span>
          <h3 className="text-lg font-semibold text-slate-900">{event.title}</h3>
          <p className="text-sm text-slate-600">{formatDateTime(event.eventDate)}</p>
          <p className="text-sm text-slate-600">{event.venue}</p>
          <p className="mt-auto pt-3 text-sm font-semibold text-slate-900">{priceLabel(event.minPrice)}</p>
        </div>
      </article>
    </Link>
  );
}
