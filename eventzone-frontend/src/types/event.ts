export interface EventSummary {
  id: string;
  title: string;
  category: string;
  eventDate: string;
  venue: string;
  coverImageUrl: string | null;
  minPrice: number | null;
  maxPrice: number | null;
}

export interface EventCategory {
  id: string;
  name: string;
}

export interface TicketCategory {
  id: string;
  name: string;
  price: number;
  totalSeats: number;
  availableSeats: number;
}

export interface EventDetail {
  id: string;
  title: string;
  description: string | null;
  category: string;
  eventDate: string;
  venue: string;
  coverImageUrl: string | null;
  ticketCategories: TicketCategory[];
}
