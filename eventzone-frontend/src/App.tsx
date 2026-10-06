import { Route, Routes } from 'react-router-dom';
import AdminPage from './pages/AdminPage';
import EventDetailPage from './pages/EventDetailPage';
import EventListPage from './pages/EventListPage';
import LoginPage from './pages/LoginPage';
import MyBookingsPage from './pages/MyBookingsPage';
import OrganiserDashboardPage from './pages/OrganiserDashboardPage';
import RegisterPage from './pages/RegisterPage';
import ProtectedRoute from './components/ProtectedRoute';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<EventListPage />} />
      <Route path="/events/:id" element={<EventDetailPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/bookings" element={<MyBookingsPage />} />
      </Route>
      <Route element={<ProtectedRoute roles={['ORGANISER']} />}>
        <Route path="/organiser" element={<OrganiserDashboardPage />} />
      </Route>
      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route path="/admin" element={<AdminPage />} />
      </Route>
    </Routes>
  );
}
