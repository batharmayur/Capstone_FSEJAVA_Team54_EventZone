import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import * as api from '../services/api';
import type { Session, User } from '../types/auth';

const STORAGE_KEY = 'eventzone.session';

interface AuthContextValue {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  /** Signs the user out when the API reports an expired or invalid token. Returns true if it did. */
  handleSessionError: (err: unknown) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function isExpired(session: Session) {
  return new Date(session.expiresAt).getTime() <= Date.now();
}

function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as Session;
    return session.token && session.user && !isExpired(session) ? session : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(loadSession);

  const clearSession = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY);
    setSession(null);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const { token, expiresAt, user } = await api.login(email, password);
    const next: Session = { token, expiresAt, user };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
    setSession(next);
  }, []);

  const register = useCallback(
    async (name: string, email: string, password: string) => {
      await api.register(name, email, password);
      await login(email, password);
    },
    [login]
  );

  const logout = useCallback(() => {
    if (session) {
      api.logout(session.token).catch(() => undefined);
    }
    clearSession();
  }, [session, clearSession]);

  const handleSessionError = useCallback(
    (err: unknown) => {
      if (err instanceof api.ApiError && err.status === 401) {
        clearSession();
        return true;
      }
      return false;
    },
    [clearSession]
  );

  // Sign the user out automatically when the token expires.
  useEffect(() => {
    if (!session) {
      return;
    }
    const remaining = new Date(session.expiresAt).getTime() - Date.now();
    const timer = window.setTimeout(clearSession, Math.max(remaining, 0));
    return () => window.clearTimeout(timer);
  }, [session, clearSession]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: session?.user ?? null,
      token: session?.token ?? null,
      login,
      register,
      logout,
      handleSessionError
    }),
    [session, login, register, logout, handleSessionError]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
