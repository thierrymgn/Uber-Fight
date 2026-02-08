"use client";

import { useEffect, useState } from "react";
import InfoSection from "@/app/utilisateurs/components/info-section";
import { collection, getDocs } from "@firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/auth-provider";
import { Utilisateur } from "@/types/utilisateur";
import UsersTable from "@/app/utilisateurs/components/users-table";

export default function UtilisateursPage() {
    const { user, loading: authLoading } = useAuth();
    const [utilisateurs, setUtilisateurs] = useState<Utilisateur[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (authLoading) return;

        if (!user) {
            setError("Vous devez être connecté pour voir cette page");
            setLoading(false);
            return;
        }

        const fetchUtilisateurs = async () => {
            try {
                setLoading(true);
                const snapshot = await getDocs(collection(db, "users"));
                const users = snapshot.docs.map(doc => ({
                    id: doc.id,
                    ...doc.data()
                })) as Utilisateur[];

                setUtilisateurs(users);
                setError(null);
            } catch (err) {
                console.error("Erreur lors de la récupération des utilisateurs:", err);
                const errorMessage = err instanceof Error ? err.message : "Erreur lors de la récupération des utilisateurs";
                setError(errorMessage);
            } finally {
                setLoading(false);
            }
        };

        fetchUtilisateurs();
    }, [user, authLoading]);

    const handleUserDeleted = (userId: string) => {
        setUtilisateurs(prevUtilisateurs =>
            prevUtilisateurs.filter(u => u.id !== userId)
        );
    };

    if (authLoading || loading) {
        return (
            <div className="p-8">
                <div className="max-w-7xl mx-auto">
                    <div className="flex items-center justify-center min-h-100">
                        <div className="text-center">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                            <p className="mt-4 text-gray-600 dark:text-gray-400">Chargement des utilisateurs...</p>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="p-8">
                <div className="max-w-7xl mx-auto">
                    <div className="bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-200 px-4 py-3 rounded">
                        <p className="font-bold">Erreur</p>
                        <p>{error}</p>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="p-8">
            <div className="max-w-7xl mx-auto">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                        Gestion des utilisateurs
                    </h1>
                    <p className="text-gray-600 dark:text-gray-400">
                        Liste complète de tous les utilisateurs inscrits
                        {utilisateurs.length === 0 && " (données de démonstration)"}
                    </p>
                </div>

                <InfoSection utilisateurs={utilisateurs}/>

                <UsersTable
                    utilisateurs={utilisateurs}
                    onUserDeleted={handleUserDeleted}
                />
            </div>
        </div>
    );
}

