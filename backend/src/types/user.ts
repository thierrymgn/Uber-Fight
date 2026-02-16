import { Timestamp } from 'firebase/firestore';
import { DocumentData } from 'firebase/firestore';

export interface UserLocation {
  latitude: number;
  longitude: number;
}

export interface User {
  id: string;
  username: string;
  email: string;
  role: string;
  createdAt: Timestamp | string;
  fcmToken: string;
  location: UserLocation | null;
  photoUrl: string;
  rating: number;
  ratingCount: number;
}

export function parseUser(id: string, data: DocumentData): User {
  return {
    id,
    username: data.username ?? 'Utilisateur inconnu',
    email: data.email ?? '',
    role: data.role ?? 'CLIENT',
    createdAt: data.createdAt ?? new Date().toISOString(),
    fcmToken: data.fcmToken ?? '',
    location: data.location ?? null,
    photoUrl: data.photoUrl ?? '',
    rating: data.rating ?? 0,
    ratingCount: data.ratingCount ?? 0,
  };
}

export function isValidUser(data: DocumentData): boolean {
  return typeof data.email === 'string' && data.email.length > 0;
}
