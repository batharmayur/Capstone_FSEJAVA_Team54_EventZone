import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import BookingModal from './BookingModal';
import { useAuth } from '../context/AuthContext';
import { ApiError, createBooking } from '../services/api';
import type { Booking } from '../types/booking';
import type { EventDetail } from '../types/event';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));
vi.mock('../services/api', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../services/api')>()),
  createBooking: vi.fn()
}));

const logout = vi.fn();
const onClose = vi.fn();
const onSeatsChanged = vi.fn();

function makeEvent(overrides: Partial<EventDetail> = {}): EventDetail {
  return {
    id: 'event-1',
    title: 'Rock Night',
    description: 'Live rock concert',
    category: 'Concert',
    eventDate: '2030-10-26T19:00:00',
    venue: 'HICC Hyderabad',
    coverImageUrl: null,
    ticketCategories: [
      { id: 'general', name: 'General', price: 999, totalSeats: 200, availableSeats: 200 },
      { id: 'vip', name: 'VIP', price: 2499, totalSeats: 50, availableSeats: 3 },
      { id: 'sold-out', name: 'Balcony', price: 1500, totalSeats: 10, availableSeats: 0 }
    ],
    ...overrides
  };
}

function makeBooking(overrides: Partial<Booking> = {}): Booking {
  return {
    id: 'booking-1',
    bookingRef: 'EZ-ABCD2345',
    eventId: 'event-1',
    eventTitle: 'Rock Night',
    eventDate: '2030-10-26T19:00:00',
    venue: 'HICC Hyderabad',
    ticketCategory: 'General',
    quantity: 2,
    totalAmount: 1998,
    status: 'CONFIRMED',
    createdAt: '2030-01-01T00:00:00Z',
    ...overrides
  };
}

function renderModal(event: EventDetail = makeEvent()) {
  return render(
    <MemoryRouter>
      <BookingModal event={event} onClose={onClose} onSeatsChanged={onSeatsChanged} />
    </MemoryRouter>
  );
}

function optionLabels(select: HTMLElement) {
  return Array.from((select as HTMLSelectElement).options).map((o) => o.textContent);
}

describe('BookingModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({ token: 'jwt-token', logout } as unknown as ReturnType<typeof useAuth>);
  });

  it('offers only categories that still have seats, with price and seats left', () => {
    renderModal();

    const ticket = screen.getByLabelText('Ticket category');
    expect(optionLabels(ticket)).toEqual(['General - ₹999 (200 left)', 'VIP - ₹2,499 (3 left)']);
    expect(screen.getByRole('button', { name: 'Confirm booking' })).toBeEnabled();
  });

  it('updates the total when the quantity changes', async () => {
    const user = userEvent.setup();
    renderModal();

    expect(screen.getByText('₹999')).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Quantity'), '2');
    expect(screen.getByText('₹1,998')).toBeInTheDocument();
  });

  it('limits quantity to 5 and to the seats left, and clamps it when the category changes', async () => {
    const user = userEvent.setup();
    renderModal();

    const quantity = screen.getByLabelText('Quantity');
    expect(optionLabels(quantity)).toEqual(['1', '2', '3', '4', '5']);

    await user.selectOptions(quantity, '5');
    await user.selectOptions(screen.getByLabelText('Ticket category'), 'vip');

    expect(optionLabels(screen.getByLabelText('Quantity'))).toEqual(['1', '2', '3']);
    expect(screen.getByLabelText('Quantity')).toHaveValue('3');
    expect(screen.getByText('₹7,497')).toBeInTheDocument();
  });

  it('books the selected category and quantity and shows the booking reference', async () => {
    const user = userEvent.setup();
    vi.mocked(createBooking).mockResolvedValue(makeBooking());
    renderModal();

    await user.selectOptions(screen.getByLabelText('Quantity'), '2');
    await user.click(screen.getByRole('button', { name: 'Confirm booking' }));

    expect(createBooking).toHaveBeenCalledWith('jwt-token', 'general', 2);
    expect(await screen.findByText('Booking confirmed')).toBeInTheDocument();
    expect(screen.getByText('EZ-ABCD2345')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View my bookings' })).toHaveAttribute('href', '/bookings');
    expect(onSeatsChanged).toHaveBeenCalledTimes(1);
  });

  it('shows the server message and refreshes seats when the booking is rejected', async () => {
    const user = userEvent.setup();
    vi.mocked(createBooking).mockRejectedValue(new ApiError(409, 'Only 1 seat(s) left in this ticket category'));
    renderModal();

    await user.click(screen.getByRole('button', { name: 'Confirm booking' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Only 1 seat(s) left in this ticket category');
    expect(screen.queryByText('Booking confirmed')).not.toBeInTheDocument();
    expect(onSeatsChanged).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: 'Confirm booking' })).toBeEnabled();
  });

  it('signs the user out and closes when the session has expired', async () => {
    const user = userEvent.setup();
    vi.mocked(createBooking).mockRejectedValue(new ApiError(401, 'Authentication is required'));
    renderModal();

    await user.click(screen.getByRole('button', { name: 'Confirm booking' }));

    await waitFor(() => expect(logout).toHaveBeenCalledTimes(1));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('disables booking when every category is sold out', () => {
    renderModal(
      makeEvent({
        ticketCategories: [{ id: 'a', name: 'General', price: 100, totalSeats: 10, availableSeats: 0 }]
      })
    );

    expect(screen.getByText('All ticket categories are sold out.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirm booking' })).toBeDisabled();
  });

  it('closes on Escape and on Cancel', async () => {
    const user = userEvent.setup();
    renderModal();

    await user.keyboard('{Escape}');
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onClose).toHaveBeenCalledTimes(2);
    expect(createBooking).not.toHaveBeenCalled();
  });
});
