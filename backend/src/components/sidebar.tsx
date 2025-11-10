import SidebarButton from "@/components/sidebar-button";
import { HomeIcon, UsersIcon, ChartIcon } from "@/components/icons";

export default function Sidebar() {
    const buttons = [
        {href: "/accueil", label: "Accueil", icon: <HomeIcon />},
        {href: "/utilisateurs", label: "Utilisateurs", icon: <UsersIcon />},
        {href: "/statistiques", label: "Statistiques", icon: <ChartIcon />},
        {href: "/parametres", label: "Paramètres", icon: <HomeIcon />},
    ];

    return (
        <aside className="w-64 bg-white dark:bg-gray-800 shadow-lg">
            <div className="p-6">
                <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">
                    Uber Fight
                </h2>
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