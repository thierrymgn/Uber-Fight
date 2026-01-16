"use client";

import { useAuth } from "@/components/providers/auth-provider";
import { usePathname } from "next/navigation";
import Sidebar from "@/components/sidebar";

export function LayoutContent({ children }: { children: React.ReactNode }) {
    const { user, loading } = useAuth();
    const pathname = usePathname();

    // Pages publiques qui ne nécessitent pas d'authentification
    const publicPages = ["/login"];
    const isPublicPage = publicPages.includes(pathname);

    // Afficher la sidebar uniquement si l'utilisateur est connecté et pas sur une page publique
    const showSidebar = user && !isPublicPage;

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-100 dark:bg-gray-900">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                    <p className="mt-4 text-gray-600 dark:text-gray-400">Chargement...</p>
                </div>
            </div>
        );
    }

    // Si l'utilisateur n'est pas connecté et n'est pas sur une page publique, on laisse le children gérer la redirection
    if (!user && !isPublicPage) {
        if (typeof window !== "undefined") {
            window.location.href = "/login";
        }
        return null;
    }

    return (
        <div className="flex min-h-screen bg-gray-100 dark:bg-gray-900">
            {showSidebar && <Sidebar />}
            <main className={showSidebar ? "flex-1" : "w-full"}>
                {children}
            </main>
        </div>
    );
}

