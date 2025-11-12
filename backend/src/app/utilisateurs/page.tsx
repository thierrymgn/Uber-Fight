import InfoSection from "@/app/utilisateurs/components/info-section";
import UserRoleTag from "@/app/utilisateurs/components/user-role-tag";

export default function UtilisateursPage() {
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
            role: "Bagarreur",
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
        <div className="p-8">
            <div className="max-w-7xl mx-auto">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                        Gestion des utilisateurs
                    </h1>
                    <p className="text-gray-600 dark:text-gray-400">
                        Liste complète de tous les utilisateurs inscrits
                    </p>
                </div>

                <InfoSection utilisateurs={utilisateurs}/>

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
                                    Date d&apos;inscription
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
                                        <UserRoleTag role={utilisateur.role}/>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                                        {new Date(utilisateur.dateInscription).toLocaleDateString('fr-FR')}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                                        <button
                                            className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 mr-3">
                                            Modifier
                                        </button>
                                        <button
                                            className="text-red-600 hover:text-red-800 dark:text-red-400 dark:hover:text-red-300">
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
        </div>
    );
}

