import { useEffect, useState } from 'react';
import CategoryTabs from '../components/CategoryTabs';
import EventCard from '../components/EventCard';
import Navbar from '../components/Navbar';
import { fetchCategories, fetchEvents } from '../services/api';
import type { EventCategory, EventSummary } from '../types/event';

export default function EventListPage() {
  const [categories, setCategories] = useState<EventCategory[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchCategories()
      .then(setCategories)
      .catch(() => setError('Unable to load categories.'));
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    fetchEvents(selectedCategory ?? undefined)
      .then((items) => {
        if (active) {
          setEvents(items);
        }
      })
      .catch(() => {
        if (active) {
          setError('Unable to load events. Please make sure the backend is running and try again.');
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
  }, [selectedCategory]);

  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-7xl px-6 py-10">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900">Upcoming events</h1>
          <p className="mt-2 text-slate-600">Browse concerts, sports, workshops and conferences.</p>
        </div>

        <div className="mb-8">
          <CategoryTabs categories={categories} selected={selectedCategory} onSelect={setSelectedCategory} />
        </div>

        {error && (
          <div role="alert" className="mb-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            {error}
          </div>
        )}

        {loading ? (
          <p className="text-slate-500">Loading events...</p>
        ) : events.length === 0 && !error ? (
          <p className="text-slate-500">No events found{selectedCategory ? ` in ${selectedCategory}` : ''}.</p>
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {events.map((event) => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
