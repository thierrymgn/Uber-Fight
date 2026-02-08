import { Timestamp } from "firebase/firestore";

export interface Utilisateur {
    id: string;
    username: string;
    email: string;
    role: string;
    createdAt: Timestamp | string;
}

