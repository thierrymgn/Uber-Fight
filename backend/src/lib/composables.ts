import { Timestamp } from "firebase/firestore";

export function formatDate(date: Timestamp | string, options?: Intl.DateTimeFormatOptions): string {
    const jsDate = date instanceof Timestamp ? date.toDate() : new Date(date);
    
    const defaultOptions: Intl.DateTimeFormatOptions = {
        year: "numeric",
        month: "long",
        day: "numeric",
    };
    
    return jsDate.toLocaleDateString("fr-FR", options ?? defaultOptions);
}

export function formatDateTime(date: Timestamp | string): string {
    return formatDate(date, {
        year: "numeric",
        month: "long",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}
