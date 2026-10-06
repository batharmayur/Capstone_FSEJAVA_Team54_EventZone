import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { Link } from 'react-router-dom';
import Navbar from './Navbar';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types/auth';

interface ProtectedRouteProps {
  /** If given, only users with one of these roles may see the page. */
  roles?: Role[];
}

/** Sends signed-out visitors to the login page and returns them here afterwards. */
export default function ProtectedRoute({ roles }: ProtectedRouteProps) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && !roles.includes(user.role)) {
    return (
      <div className="min-h-screen">
        <Navbar />
        <main className="mx-auto max-w-xl px-6 py-16 text-center">
          <h1 className="text-2xl font-bold text-slate-900">Access denied</h1>
          <p className="mt-3 text-slate-600">Your account does not have permission to view this page.</p>
          <Link to="/" className="btn-primary mt-6">Back to events</Link>
        </main>
      </div>
    );
  }

  return <Outlet />;
}
