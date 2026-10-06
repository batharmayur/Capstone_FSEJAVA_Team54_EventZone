import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ProtectedRoute from './ProtectedRoute';
import { useAuth } from '../context/AuthContext';
import type { Role, User } from '../types/auth';

vi.mock('../context/AuthContext', () => ({ useAuth: vi.fn() }));

function signIn(role: Role | null) {
  const user: User | null = role ? { id: 'u1', email: 'a@eventzone.com', name: 'Test User', role } : null;
  vi.mocked(useAuth).mockReturnValue({ user, logout: vi.fn() } as unknown as ReturnType<typeof useAuth>);
}

function renderAt(path: string, roles?: Role[]) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<p>Login page</p>} />
        <Route element={<ProtectedRoute roles={roles} />}>
          <Route path="/secret" element={<p>Secret content</p>} />
        </Route>
      </Routes>
    </MemoryRouter>
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('redirects signed-out visitors to the login page', () => {
    signIn(null);
    renderAt('/secret');

    expect(screen.getByText('Login page')).toBeInTheDocument();
    expect(screen.queryByText('Secret content')).not.toBeInTheDocument();
  });

  it('lets any signed-in user through when no roles are required', () => {
    signIn('ATTENDEE');
    renderAt('/secret');

    expect(screen.getByText('Secret content')).toBeInTheDocument();
  });

  it('lets a user with an allowed role through', () => {
    signIn('ADMIN');
    renderAt('/secret', ['ADMIN']);

    expect(screen.getByText('Secret content')).toBeInTheDocument();
  });

  it('shows "Access denied" to a signed-in user with the wrong role', () => {
    signIn('ATTENDEE');
    renderAt('/secret', ['ADMIN', 'ORGANISER']);

    expect(screen.getByRole('heading', { name: 'Access denied' })).toBeInTheDocument();
    expect(screen.queryByText('Secret content')).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to events' })).toHaveAttribute('href', '/');
  });
});
