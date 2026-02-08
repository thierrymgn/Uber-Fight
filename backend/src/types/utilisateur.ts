import { Timestamp } from "firebase/firestore";
import { DocumentData } from "firebase/firestore";

export interface Utilisateur {
    id: string;
    username: string;
    email: string;
    role: string;
    createdAt: Timestamp | string;
}

export function parseUtilisateur(id: string, data: DocumentData): Utilisateur {
    return {
        id,
        username: data.username ?? "Utilisateur inconnu",
        email: data.email ?? "",
        role: data.role ?? "CLIENT",
        createdAt: data.createdAt ?? new Date().toISOString(),
    };
}

export function isValidUtilisateur(data: DocumentData): boolean {
    return typeof data.email === "string" && data.email.length > 0;
}

