export interface IInfoSectionProps {
    utilisateurs: {
        id: number;
        nom: string;
        prenom: string;
        email: string;
        role: string;
        dateInscription: string;
    }[];
}

export default function InfoSection({utilisateurs}: IInfoSectionProps)
{
    return (
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
    )
}