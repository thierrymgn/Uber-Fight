'use client';

import { useAuth } from '@/components/providers/auth-provider';
import Link from 'next/link';

export default function Home() {
  const { user } = useAuth();

  return (
    <div className="font-sans min-h-screen p-8 pb-20">
      <div className="max-w-4xl mx-auto">
        <div className="flex flex-col gap-8 items-center justify-center min-h-[80vh]">
          <h1 className="text-4xl font-bold text-gray-900 dark:text-white text-center">
            Bienvenue sur Uber Fight
          </h1>
          {user && (
            <p className="text-lg text-blue-600 dark:text-blue-400 text-center">
              Connecté en tant que : <span className="font-semibold">{user.email}</span>
            </p>
          )}
          <p className="text-lg text-gray-600 dark:text-gray-400 text-center max-w-2xl">
            Application de gestion Uber Fight. Utilisez la sidebar pour naviguer entre les
            différentes sections.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mt-8 w-full max-w-2xl">
            <Link
              href="/utilisateurs"
              className="block p-6 bg-white dark:bg-gray-800 rounded-lg shadow hover:shadow-lg transition-shadow"
            >
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                👥 Utilisateurs
              </h2>
              <p className="text-gray-600 dark:text-gray-400">
                Gérez tous les utilisateurs de l&#39;application
              </p>
            </Link>

            <Link
              href="/dashboard"
              className="block p-6 bg-white dark:bg-gray-800 rounded-lg shadow hover:shadow-lg transition-shadow"
            >
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                📊 Statistiques
              </h2>
              <p className="text-gray-600 dark:text-gray-400">
                Consultez les statistiques et analyses
              </p>
            </Link>

            {/* <Link
              href="/parametres"
              className="block p-6 bg-white dark:bg-gray-800 rounded-lg shadow hover:shadow-lg transition-shadow"
            >
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                ⚙️ Paramètres
              </h2>
              <p className="text-gray-600 dark:text-gray-400">
                Configurez les paramètres de l&#39;application
              </p>
            </Link> */}
          </div>
        </div>
      </div>
    </div>
  );
}
