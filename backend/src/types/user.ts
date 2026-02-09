import { Timestamp } from "firebase/firestore";
import { DocumentData } from "firebase/firestore";

export interface User {
    id: string;
    username: string;
    email: string;
    role: string;
    createdAt: Timestamp | string;
}

export function parseUser(id: string, data: DocumentData): User {
    return {
        id,
        username: data.username ?? "Utilisateur inconnu",
        email: data.email ?? "",
        role: data.role ?? "CLIENT",
        createdAt: data.createdAt ?? new Date().toISOString(),
    };
}

export function isValidUser(data: DocumentData): boolean {
    return typeof data.email === "string" && data.email.length > 0;
}

