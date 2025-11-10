export default function UtilisateursPage() {
  // Données utilisateurs factices pour le moment
  const utilisateurs = [
    {
      id: 1,
      nom: "Dupont",
      prenom: "Jean",
      email: "jean.dupont@example.com",
      role: "Administrateur",
      dateInscription: "2024-01-15"
    },
    {
      id: 2,
      nom: "Martin",
      prenom: "Sophie",
      email: "sophie.martin@example.com",
      role: "Utilisateur",
      dateInscription: "2024-02-20"
    },
    {
      id: 3,
      nom: "Bernard",
      prenom: "Pierre",
      email: "pierre.bernard@example.com",
      role: "Modérateur",
      dateInscription: "2024-03-10"
    },
    {
      id: 4,
      nom: "Dubois",
      prenom: "Marie",
      email: "marie.dubois@example.com",
      role: "Utilisateur",
      dateInscription: "2024-04-05"
    },
    {
      id: 5,
      nom: "Leroy",
      prenom: "Thomas",
      email: "thomas.leroy@example.com",
      role: "Utilisateur",
      dateInscription: "2024-05-12"
    }
  ];

  return (
    <div className="flex min-h-screen bg-gray-100 dark:bg-gray-900">
      {/* Sidebar */}
      <aside className="w-64 bg-white dark:bg-gray-800 shadow-lg">
        <div className="p-6">
          <h2 className="text-2xl font-bold text-gray-800 dark:text-white mb-6">
            Uber Fight
          </h2>
          <nav className="space-y-2">
            <a
              href="/"
              className="block px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
            >
              🏠 Accueil
            </a>
            <a
              href="/utilisateurs"
              className="block px-4 py-2 bg-blue-500 text-white rounded font-medium"
            >
              👥 Utilisateurs
            </a>
            <a
              href="#"
              className="block px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
            >
              📊 Statistiques
            </a>
            <a
              href="#"
              className="block px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition-colors"
            >
              ⚙️ Paramètres
            </a>
          </nav>
        </div>
      </aside>

      {/* Contenu principal */}
      <main className="flex-1 p-8">
        <div className="max-w-7xl mx-auto">
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
              Gestion des utilisateurs
            </h1>
            <p className="text-gray-600 dark:text-gray-400">
              Liste complète de tous les utilisateurs inscrits
            </p>
          </div>

          {/* Statistiques rapides */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
              <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">
                Total utilisateurs
              </div>
              <div className="text-3xl font-bold text-gray-900 dark:text-white">
                {utilisateurs.length}
              </div>
            </div>
            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
              <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">
                Administrateurs
              </div>
              <div className="text-3xl font-bold text-gray-900 dark:text-white">
                {utilisateurs.filter(u => u.role === "Administrateur").length}
              </div>
            </div>
            <div className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow">
              <div className="text-sm text-gray-600 dark:text-gray-400 mb-1">
                Utilisateurs actifs
              </div>
              <div className="text-3xl font-bold text-gray-900 dark:text-white">
                {utilisateurs.filter(u => u.role === "Utilisateur").length}
              </div>
            </div>
          </div>

          {/* Tableau des utilisateurs */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 dark:bg-gray-700">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Nom
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Prénom
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Email
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Rôle
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Date d'inscription
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                  {utilisateurs.map((utilisateur) => (
                    <tr
                      key={utilisateur.id}
                      className="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors"
                    >
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100">
                        {utilisateur.id}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">
                        {utilisateur.nom}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-gray-100">
                        {utilisateur.prenom}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                        {utilisateur.email}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm">
                        <span
                          className={`px-2 py-1 rounded-full text-xs font-medium ${
                            utilisateur.role === "Administrateur"
                              ? "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200"
                              : utilisateur.role === "Modérateur"
                              ? "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200"
                              : "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200"
                          }`}
                        >
                          {utilisateur.role}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                        {new Date(utilisateur.dateInscription).toLocaleDateString('fr-FR')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                        <button className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 mr-3">
                          Modifier
                        </button>
                        <button className="text-red-600 hover:text-red-800 dark:text-red-400 dark:hover:text-red-300">
                          Supprimer
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

