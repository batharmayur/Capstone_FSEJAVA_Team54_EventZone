export type Role = 'ATTENDEE' | 'ORGANISER' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  name: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: User;
}

export interface Session {
  token: string;
  expiresAt: string;
  user: User;
}
