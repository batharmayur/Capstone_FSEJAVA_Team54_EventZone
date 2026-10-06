import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import type { User } from '../types/auth';

interface ProfileMenuProps {
  user: User;
  onLogout: () => void;
}

const ROLE_LABEL: Record<User['role'], string> = {
  ATTENDEE: 'Attendee',
  ORGANISER: 'Organiser',
  ADMIN: 'Admin'
};

function initials(name: string) {
  return (
    name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((word) => word.charAt(0).toUpperCase())
      .join('') || '?'
  );
}

const itemClass =
  'block w-full rounded-lg px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-100 focus:outline-none focus-visible:bg-slate-100';

export default function ProfileMenu({ user, onLogout }: ProfileMenuProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuId = useId();
  const location = useLocation();

  const dashboard =
    user.role === 'ORGANISER'
      ? { to: '/organiser', label: 'My events' }
      : user.role === 'ADMIN'
        ? { to: '/admin', label: 'Admin panel' }
        : null;

  function menuItems() {
    return Array.from(containerRef.current?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? []);
  }

  function close(returnFocus = false) {
    setOpen(false);
    if (returnFocus) {
      triggerRef.current?.focus();
    }
  }

  // Close when navigating to another page.
  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);

  // Close on a click outside the menu.
  useEffect(() => {
    if (!open) {
      return;
    }
    function onMouseDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onMouseDown);
    return () => document.removeEventListener('mousedown', onMouseDown);
  }, [open]);

  // Move focus into the menu when it opens.
  useEffect(() => {
    if (open) {
      menuItems()[0]?.focus();
    }
  }, [open]);

  function onKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (!open) {
      if (event.key === 'ArrowDown' && event.target === triggerRef.current) {
        event.preventDefault();
        setOpen(true);
      }
      return;
    }

    const items = menuItems();
    const index = items.indexOf(document.activeElement as HTMLElement);

    switch (event.key) {
      case 'Escape':
        event.preventDefault();
        close(true);
        break;
      case 'Tab':
        setOpen(false);
        break;
      case 'ArrowDown':
        event.preventDefault();
        items[(index + 1) % items.length]?.focus();
        break;
      case 'ArrowUp':
        event.preventDefault();
        items[(index - 1 + items.length) % items.length]?.focus();
        break;
      case 'Home':
        event.preventDefault();
        items[0]?.focus();
        break;
      case 'End':
        event.preventDefault();
        items[items.length - 1]?.focus();
        break;
      default:
    }
  }

  return (
    <div ref={containerRef} className="relative" onKeyDown={onKeyDown}>
      <button
        ref={triggerRef}
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        aria-label={`Account menu for ${user.name}`}
        className="flex items-center gap-2 rounded-full border border-slate-200 bg-white py-1 pl-1 pr-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-300"
        onClick={() => setOpen((value) => !value)}
      >
        <span aria-hidden="true" className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 text-xs font-bold text-white">
          {initials(user.name)}
        </span>
        <span className="hidden max-w-[10rem] truncate sm:inline">{user.name}</span>
        <svg aria-hidden="true" viewBox="0 0 20 20" className={`h-4 w-4 text-slate-500 transition ${open ? 'rotate-180' : ''}`} fill="currentColor">
          <path fillRule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.17l3.71-3.94a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clipRule="evenodd" />
        </svg>
      </button>

      {open && (
        <div
          id={menuId}
          role="menu"
          aria-label="Account"
          className="absolute right-0 z-40 mt-2 w-64 rounded-xl border border-slate-200 bg-white p-2 shadow-lg"
        >
          <div className="px-3 py-2">
            <p className="truncate text-sm font-semibold text-slate-900">{user.name}</p>
            <p className="truncate text-xs text-slate-500">{user.email}</p>
            <span className="mt-2 inline-block rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-700">
              {ROLE_LABEL[user.role]}
            </span>
          </div>
          <div role="separator" className="my-1 border-t border-slate-200" />
          {dashboard && (
            <Link role="menuitem" to={dashboard.to} className={itemClass} onClick={() => close()}>
              {dashboard.label}
            </Link>
          )}
          <Link role="menuitem" to="/bookings" className={itemClass} onClick={() => close()}>
            My bookings
          </Link>
          <div role="separator" className="my-1 border-t border-slate-200" />
          <button
            type="button"
            role="menuitem"
            className={`${itemClass} text-red-600 hover:bg-red-50 focus-visible:bg-red-50`}
            onClick={() => {
              close();
              onLogout();
            }}
          >
            Log out
          </button>
        </div>
      )}
    </div>
  );
}
