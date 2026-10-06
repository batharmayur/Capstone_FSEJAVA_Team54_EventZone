export interface EventPayload {
  title: string;
  description: string | null;
  /** ISO local date-time, e.g. 2026-11-01T19:00:00 */
  eventDate: string;
  venue: string;
  coverImageUrl: string | null;
  categoryId: string;
}

export interface TicketPayload {
  name: string;
  price: number;
  totalSeats: number;
}

export interface OrganiserTicket {
  id: string;
  name: string;
  price: number;
  totalSeats: number;
  availableSeats: number;
  bookedSeats: number;
  bookingCount: number;
}

export interface OrganiserEvent {
  id: string;
  title: string;
  description: string | null;
  categoryId: string;
  category: string;
  eventDate: string;
  venue: string;
  coverImageUrl: string | null;
  active: boolean;
  ticketCategories: OrganiserTicket[];
}

export interface AdminEvent {
  id: string;
  title: string;
  category: string;
  eventDate: string;
  venue: string;
  active: boolean;
  organiser: string | null;
}
