import type { AuthResponse, User } from '../types/auth';
import type { Booking } from '../types/booking';
import type { EventCategory, EventDetail, EventSummary } from '../types/event';
import type {
  AdminEvent,
  EventPayload,
  OrganiserEvent,
  OrganiserTicket,
  TicketPayload
} from '../types/management';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
  }
}

/** The server's message for API errors, otherwise a friendly fallback. */
export function errorMessage(err: unknown, fallback: string): string {
  return err instanceof ApiError ? err.message : fallback;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, init);

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = (await response.json()) as { message?: string };
      if (body.message) {
        message = body.message;
      }
    } catch {
      // Response had no JSON body; keep the generic message.
    }
    throw new ApiError(response.status, message);
  }

  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

function getJson<T>(path: string, token?: string): Promise<T> {
  return request<T>(path, token ? { headers: { Authorization: `Bearer ${token}` } } : undefined);
}

function sendJson<T>(
  method: 'POST' | 'PUT' | 'DELETE',
  path: string,
  body?: unknown,
  token?: string
): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return request<T>(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
}

export function login(email: string, password: string): Promise<AuthResponse> {
  return sendJson<AuthResponse>('POST', '/api/auth/login', { email, password });
}

export function register(name: string, email: string, password: string): Promise<User> {
  return sendJson<User>('POST', '/api/auth/register', { name, email, password });
}

export function logout(token: string): Promise<void> {
  return sendJson<void>('POST', '/api/auth/logout', undefined, token);
}

export function createBooking(token: string, ticketCategoryId: string, quantity: number): Promise<Booking> {
  return sendJson<Booking>('POST', '/api/bookings', { ticketCategoryId, quantity }, token);
}

export function fetchMyBookings(token: string): Promise<Booking[]> {
  return getJson<Booking[]>('/api/bookings/mine', token);
}

export function cancelBooking(token: string, id: string): Promise<Booking> {
  return sendJson<Booking>('PUT', `/api/bookings/${encodeURIComponent(id)}/cancel`, undefined, token);
}

const enc = encodeURIComponent;

// Organiser
export function fetchOrganiserEvents(token: string): Promise<OrganiserEvent[]> {
  return getJson<OrganiserEvent[]>('/api/organiser/events', token);
}

export function createEvent(token: string, payload: EventPayload): Promise<OrganiserEvent> {
  return sendJson<OrganiserEvent>('POST', '/api/events', payload, token);
}

export function updateEvent(token: string, id: string, payload: EventPayload): Promise<OrganiserEvent> {
  return sendJson<OrganiserEvent>('PUT', `/api/events/${enc(id)}`, payload, token);
}

export function deleteEvent(token: string, id: string): Promise<void> {
  return sendJson<void>('DELETE', `/api/events/${enc(id)}`, undefined, token);
}

export function addTicketCategory(token: string, eventId: string, payload: TicketPayload): Promise<OrganiserTicket> {
  return sendJson<OrganiserTicket>('POST', `/api/events/${enc(eventId)}/ticket-categories`, payload, token);
}

export function updateTicketCategory(token: string, id: string, payload: TicketPayload): Promise<OrganiserTicket> {
  return sendJson<OrganiserTicket>('PUT', `/api/ticket-categories/${enc(id)}`, payload, token);
}

export function deleteTicketCategory(token: string, id: string): Promise<void> {
  return sendJson<void>('DELETE', `/api/ticket-categories/${enc(id)}`, undefined, token);
}

// Admin
export function fetchAdminEvents(token: string): Promise<AdminEvent[]> {
  return getJson<AdminEvent[]>('/api/admin/events', token);
}

export function setEventActive(token: string, id: string, active: boolean): Promise<AdminEvent> {
  return sendJson<AdminEvent>('PUT', `/api/admin/events/${enc(id)}/active`, { active }, token);
}

export function createCategory(token: string, name: string): Promise<EventCategory> {
  return sendJson<EventCategory>('POST', '/api/admin/categories', { name }, token);
}

export function updateCategory(token: string, id: string, name: string): Promise<EventCategory> {
  return sendJson<EventCategory>('PUT', `/api/admin/categories/${enc(id)}`, { name }, token);
}

export function deleteCategory(token: string, id: string): Promise<void> {
  return sendJson<void>('DELETE', `/api/admin/categories/${enc(id)}`, undefined, token);
}

export function fetchEvent(id: string): Promise<EventDetail> {
  return getJson<EventDetail>(`/api/events/${encodeURIComponent(id)}`);
}

export function fetchEvents(category?: string): Promise<EventSummary[]> {
  const query = category ? `?category=${encodeURIComponent(category)}` : '';
  return getJson<EventSummary[]>(`/api/events${query}`);
}

export function fetchCategories(): Promise<EventCategory[]> {
  return getJson<EventCategory[]>('/api/categories');
}
