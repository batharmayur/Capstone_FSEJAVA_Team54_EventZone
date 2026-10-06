import { useCallback, useEffect, useState, type FormEvent } from 'react';
import Navbar from '../components/Navbar';
import { useAuth } from '../context/AuthContext';
import * as api from '../services/api';
import type { EventCategory } from '../types/event';
import type { AdminEvent } from '../types/management';
import { formatDateTime } from '../utils/format';

const inputClass = 'rounded-xl border border-slate-200 bg-white px-3 py-2 text-slate-800';

export default function AdminPage() {
  const { token, handleSessionError } = useAuth();
  const [categories, setCategories] = useState<EventCategory[]>([]);
  const [events, setEvents] = useState<AdminEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [newName, setNewName] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState('');

  const load = useCallback(async () => {
    if (!token) {
      return;
    }
    try {
      const [cats, evts] = await Promise.all([api.fetchCategories(), api.fetchAdminEvents(token)]);
      setCategories(cats);
      setEvents(evts);
    } catch (err) {
      if (!handleSessionError(err)) {
        setError(api.errorMessage(err, 'Unable to load admin data. Please make sure the backend is running.'));
      }
    } finally {
      setLoading(false);
    }
  }, [token, handleSessionError]);

  useEffect(() => {
    void load();
  }, [load]);

  async function run(action: () => Promise<unknown>, fallback: string): Promise<boolean> {
    setError(null);
    try {
      await action();
      await load();
      return true;
    } catch (err) {
      if (!handleSessionError(err)) {
        setError(api.errorMessage(err, fallback));
      }
      return false;
    }
  }

  async function handleAdd(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (await run(() => api.createCategory(token!, newName.trim()), 'Unable to add the category.')) {
      setNewName('');
    }
  }

  async function handleRename(id: string) {
    if (await run(() => api.updateCategory(token!, id, editName.trim()), 'Unable to rename the category.')) {
      setEditingId(null);
    }
  }

  function handleDelete(category: EventCategory) {
    if (window.confirm(`Delete the "${category.name}" category?`)) {
      void run(() => api.deleteCategory(token!, category.id), 'Unable to delete the category.');
    }
  }

  return (
    <div className="min-h-screen">
      <Navbar />
      <main className="mx-auto max-w-6xl px-6 py-10">
        <h1 className="text-3xl font-bold text-slate-900">Admin panel</h1>

        {error && (
          <div role="alert" className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
        )}

        {loading ? (
          <p className="mt-6 text-slate-500">Loading...</p>
        ) : (
          <>
            <section className="card-surface mt-8 p-6" aria-labelledby="categories-heading">
              <h2 id="categories-heading" className="text-xl font-semibold text-slate-900">Event categories</h2>
              <form className="mt-4 flex flex-wrap gap-3" onSubmit={handleAdd}>
                <label htmlFor="new-category" className="sr-only">New category name</label>
                <input id="new-category" required maxLength={100} value={newName} onChange={(e) => setNewName(e.target.value)} placeholder="New category name" className={`${inputClass} flex-1 min-w-[12rem]`} />
                <button type="submit" className="btn-primary" disabled={!newName.trim()}>Add category</button>
              </form>

              <ul className="mt-4 divide-y divide-slate-200">
                {categories.map((category) => (
                  <li key={category.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                    {editingId === category.id ? (
                      <>
                        <label htmlFor={`rename-${category.id}`} className="sr-only">Category name</label>
                        <input id={`rename-${category.id}`} maxLength={100} value={editName} onChange={(e) => setEditName(e.target.value)} className={`${inputClass} flex-1 min-w-[12rem]`} />
                        <div className="flex gap-2">
                          <button type="button" className="btn-primary" disabled={!editName.trim()} onClick={() => handleRename(category.id)}>Save</button>
                          <button type="button" className="btn-secondary" onClick={() => setEditingId(null)}>Cancel</button>
                        </div>
                      </>
                    ) : (
                      <>
                        <span className="font-medium text-slate-900">{category.name}</span>
                        <div className="flex gap-2">
                          <button type="button" className="btn-secondary" onClick={() => { setEditingId(category.id); setEditName(category.name); }}>Rename</button>
                          <button type="button" className="btn-secondary" onClick={() => handleDelete(category)}>Delete</button>
                        </div>
                      </>
                    )}
                  </li>
                ))}
              </ul>
            </section>

            <section className="card-surface mt-8 overflow-hidden" aria-labelledby="events-heading">
              <h2 id="events-heading" className="p-6 pb-4 text-xl font-semibold text-slate-900">All events</h2>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
                  <thead className="bg-slate-50 text-slate-500">
                    <tr>
                      <th className="px-6 py-3 font-medium">Event</th>
                      <th className="px-6 py-3 font-medium">Category</th>
                      <th className="px-6 py-3 font-medium">Organiser</th>
                      <th className="px-6 py-3 font-medium">Date</th>
                      <th className="px-6 py-3 font-medium">Status</th>
                      <th className="px-6 py-3 font-medium"><span className="sr-only">Actions</span></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 bg-white text-slate-700">
                    {events.map((event) => (
                      <tr key={event.id}>
                        <td className="px-6 py-3 font-medium text-slate-900">{event.title}</td>
                        <td className="px-6 py-3">{event.category}</td>
                        <td className="px-6 py-3">{event.organiser ?? '-'}</td>
                        <td className="px-6 py-3">{formatDateTime(event.eventDate)}</td>
                        <td className="px-6 py-3">
                          <span className={event.active ? 'rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700' : 'rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600'}>
                            {event.active ? 'Active' : 'Deactivated'}
                          </span>
                        </td>
                        <td className="px-6 py-3 text-right">
                          <button type="button" className="btn-secondary" onClick={() => run(() => api.setEventActive(token!, event.id, !event.active), 'Unable to update the event.')}>
                            {event.active ? 'Deactivate' : 'Activate'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
