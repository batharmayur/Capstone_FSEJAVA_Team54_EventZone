import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ProfileMenu from './ProfileMenu';
import type { Role, User } from '../types/auth';

const onLogout = vi.fn();

function makeUser(role: Role = 'ATTENDEE', overrides: Partial<User> = {}): User {
  return { id: 'u1', email: 'divya@eventzone.com', name: 'Divya Rao', role, ...overrides };
}

function renderMenu(user: User) {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<><ProfileMenu user={user} onLogout={onLogout} /><button type="button">Elsewhere</button></>} />
        <Route path="/bookings" element={<p>Bookings page</p>} />
        <Route path="/organiser" element={<p>Organiser page</p>} />
        <Route path="/admin" element={<p>Admin page</p>} />
      </Routes>
    </MemoryRouter>
  );
}

const trigger = () => screen.getByRole('button', { name: /account menu for/i });

describe('ProfileMenu', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('is closed by default and shows the initials and name on the trigger', () => {
    renderMenu(makeUser());

    expect(trigger()).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(screen.getByText('DR')).toBeInTheDocument();
  });

  it('opens on click and shows the account details', async () => {
    const user = userEvent.setup();
    renderMenu(makeUser('ATTENDEE'));

    await user.click(trigger());

    expect(trigger()).toHaveAttribute('aria-expanded', 'true');
    const menu = screen.getByRole('menu', { name: 'Account' });
    expect(menu).toHaveTextContent('Divya Rao');
    expect(menu).toHaveTextContent('divya@eventzone.com');
    expect(menu).toHaveTextContent('Attendee');
  });

  it('lists only the links that apply to the role', async () => {
    const user = userEvent.setup();
    const { unmount } = renderMenu(makeUser('ATTENDEE'));
    await user.click(trigger());
    expect(screen.getAllByRole('menuitem').map((i) => i.textContent)).toEqual(['My bookings', 'Log out']);
    unmount();

    const organiser = renderMenu(makeUser('ORGANISER'));
    await user.click(trigger());
    expect(screen.getAllByRole('menuitem').map((i) => i.textContent)).toEqual(['My events', 'My bookings', 'Log out']);
    organiser.unmount();

    renderMenu(makeUser('ADMIN'));
    await user.click(trigger());
    expect(screen.getAllByRole('menuitem').map((i) => i.textContent)).toEqual(['Admin panel', 'My bookings', 'Log out']);
  });

  it('toggles closed when the trigger is clicked again', async () => {
    const user = userEvent.setup();
    renderMenu(makeUser());

    await user.click(trigger());
    await user.click(trigger());

    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('closes on Escape and returns focus to the trigger', async () => {
    const user = userEvent.setup();
    renderMenu(makeUser());

    await user.click(trigger());
    await user.keyboard('{Escape}');

    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(trigger()).toHaveFocus();
  });

  it('closes when clicking outside', async () => {
    const user = userEvent.setup();
    renderMenu(makeUser());

    await user.click(trigger());
    await user.click(screen.getByRole('button', { name: 'Elsewhere' }));

    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('focuses the first item on open and moves with the arrow keys, wrapping around', async () => {
    const user = userEvent.setup();
    renderMenu(makeUser('ORGANISER'));

    await user.click(trigger());
    const [events, bookings, logout] = screen.getAllByRole('menuitem');
    expect(events).toHaveFocus();

    await user.keyboard('{ArrowDown}');
    expect(bookings).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(logout).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(events).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(logout).toHaveFocus();
    await user.keyboard('{Home}');
    expect(events).toHaveFocus();
    await user.keyboard('{End}');
    expect(logout).toHaveFocus();
  });

  it('opens with ArrowDown from the trigger', async () => {
    const user = userEvent.setup();
    renderMenu(makeUser());

    trigger().focus();
    await user.keyboard('{ArrowDown}');

    expect(screen.getByRole('menu')).toBeInTheDocument();
  });

  it('navigates and closes when a link is chosen', async () => {
    const user = userEvent.setup();
    renderMenu(makeUser('ADMIN'));

    await user.click(trigger());
    await user.click(screen.getByRole('menuitem', { name: 'Admin panel' }));

    expect(screen.getByText('Admin page')).toBeInTheDocument();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('logs out and closes the menu', async () => {
    const user = userEvent.setup();
    renderMenu(makeUser());

    await user.click(trigger());
    await user.click(screen.getByRole('menuitem', { name: 'Log out' }));

    expect(onLogout).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('falls back to a placeholder when the name is empty', () => {
    renderMenu(makeUser('ATTENDEE', { name: '  ' }));

    expect(screen.getByText('?')).toBeInTheDocument();
  });
});
