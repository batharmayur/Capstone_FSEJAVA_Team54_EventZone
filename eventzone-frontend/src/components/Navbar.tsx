import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <header className="border-b border-slate-200 bg-white/80 backdrop-blur">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
        <Link to="/" className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-600 font-bold text-white">E</div>
          <span className="text-lg font-bold text-slate-900">EventZone</span>
        </Link>
        <nav className="flex items-center gap-3">
          {user ? (
            <>
              {user.role === 'ORGANISER' && (
                <Link to="/organiser" className="text-sm font-medium text-slate-700 hover:text-brand-700">My Events</Link>
              )}
              {user.role === 'ADMIN' && (
                <Link to="/admin" className="text-sm font-medium text-slate-700 hover:text-brand-700">Admin</Link>
              )}
              <Link to="/bookings" className="text-sm font-medium text-slate-700 hover:text-brand-700">My Bookings</Link>
              <span className="hidden text-sm text-slate-600 sm:inline">
                {user.name} <span className="text-xs uppercase text-slate-400">({user.role})</span>
              </span>
              <button type="button" className="btn-secondary" onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn-secondary">Login</Link>
              <Link to="/register" className="btn-primary">Register</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
