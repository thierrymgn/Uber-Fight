import Link from "next/link";

export default function Sidebar() {
    return (
        <aside className="w-64 bg-white dark:bg-gray-800 shadow-lg">
            <div className="p-6">
                <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">
                    Uber Fight
                </h2>
                <nav className="space-y-2">
                    <Link
                        href="/"
                        className="block px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
                    >
                        🏠 Accueil
                    </Link>
                    <Link
                        href="/utilisateurs"
                        className="block px-4 py-2 bg-blue-500 text-white rounded font-medium"
                    >
                        👥 Utilisateurs
                    </Link>
                    <Link
                        href="#"
                        className="block px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
                    >
                        📊 Statistiques
                    </Link>
                    <Link
                        href="#"
                        className="block px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
                    >
                        ⚙️ Paramètres
                    </Link>
                </nav>
            </div>
        </aside>
    )
}