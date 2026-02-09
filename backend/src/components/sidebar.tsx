'use client';

import SidebarButton from "@/components/sidebar-button";
import { HomeIcon, UsersIcon, ChartIcon, LogoutIcon } from "@/components/icons";
import Image from "next/image";
import { useAuth } from "@/components/providers/auth-provider";
import { useRouter } from "next/navigation";

export default function Sidebar() {
    const { user, logout } = useAuth();
    const router = useRouter();

    const buttons = [
        {href: "/", label: "Accueil", icon: <HomeIcon />},
        {href: "/dashboard", label: "Dashboard", icon: <ChartIcon />},
        {href: "/utilisateurs", label: "Utilisateurs", icon: <UsersIcon />},
        {href: "/parametres", label: "Paramètres", icon: <HomeIcon />},
    ];

    const handleLogout = async () => {
        try {
            await logout();
            router.push("/login");
        } catch (error) {
            console.error("Erreur lors de la déconnexion:", error);
        }
    };

    return (
        <aside className="w-64 bg-white dark:bg-gray-800 shadow-lg flex flex-col h-screen">
            <div className="p-4 flex-1">
                <div className="mb-6">
                    <Image
                        src="/logo/ic_logo_uber_fight.png"
                        alt="Uber Fight Logo"
                        width={180}
                        height={60}
                        className="w-full h-auto"
                        priority
                    />
                </div>
                <nav className="space-y-2">
                    {
                        buttons.map((button) => (
                            <SidebarButton
                                key={button.href}
                                label={button.label}
                                href={button.href}
                                icon={button.icon}
                            />
                        ))
                    }
                </nav>
            </div>

            {/* Section utilisateur et déconnexion */}
            <div className="p-4 border-t border-gray-200 dark:border-gray-700">
                {user && (
                    <div className="mb-3">
                        <p className="text-xs text-gray-500 dark:text-gray-400">Connecté en tant que</p>
                        <p className="text-sm font-medium text-gray-900 dark:text-white truncate">
                            {user.email}
                        </p>
                    </div>
                )}
                <button
                    onClick={handleLogout}
                    className="w-full flex items-center space-x-3 px-4 py-2 text-gray-700 dark:text-gray-200 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg transition-colors duration-200 group"
                >
                    <LogoutIcon className="text-red-600 dark:text-red-400" />
                    <span className="font-medium text-red-600 dark:text-red-400">Déconnexion</span>
                </button>
            </div>
        </aside>
    )
}