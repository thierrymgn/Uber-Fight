'use client';

import SidebarButton from "@/components/sidebar-button";
import { HomeIcon, UsersIcon, ChartIcon } from "@/components/icons";
import Image from "next/image";

export default function Sidebar() {
    const buttons = [
        {href: "/", label: "Accueil", icon: <HomeIcon />},
        {href: "/utilisateurs", label: "Utilisateurs", icon: <UsersIcon />},
        {href: "/statistiques", label: "Statistiques", icon: <ChartIcon />},
        {href: "/parametres", label: "Paramètres", icon: <HomeIcon />},
    ];

    return (
        <aside className="w-64 bg-white dark:bg-gray-800 shadow-lg">
            <div className="p-4">
                <div>
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
        </aside>
    )
}