export type BookingStatus = 'CONFIRMED' | 'CANCELLED';

export interface Booking {
  id: string;
  bookingRef: string;
  eventId: string;
  eventTitle: string;
  eventDate: string;
  venue: string;
  ticketCategory: string;
  quantity: number;
  totalAmount: number;
  status: BookingStatus;
  createdAt: string;
}
