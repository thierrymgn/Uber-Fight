"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { doc, getDoc } from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { db, functions } from "@/lib/firebase";
import { useAuth } from "@/components/providers/auth-provider";
import { Utilisateur } from "@/types/utilisateur";
import { formatDateTime } from "@/lib/utils";

interface UpdateUserResponse {
    success: boolean;
    message: string;
}

interface FormData {
    username: string;
    email: string;
    role: string;
}

export default function EditUtilisateurPage() {
    const params = useParams();
    const router = useRouter();
    const { user, loading: authLoading } = useAuth();
    const [utilisateur, setUtilisateur] = useState<Utilisateur | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    const [formData, setFormData] = useState<FormData>({
        username: "",
        email: "",
        role: "",
    });

    useEffect(() => {
        if (authLoading) return;

        if (!user) {
            router.push("/login");
            return;
        }

        const fetchUtilisateur = async () => {
            try {
                setLoading(true);
                const userId = params.id as string;
                const docRef = doc(db, "users", userId);
                const docSnap = await getDoc(docRef);

                if (docSnap.exists()) {
                    const userData = {
                        id: docSnap.id,
                        ...docSnap.data()
                    } as Utilisateur;

                    setUtilisateur(userData);
                    setFormData({
                        username: userData.username,
                        email: userData.email,
                        role: userData.role,
                    });
                    setError(null);
                } else {
                    setError("Utilisateur non trouvé");
                }
            } catch (err) {
                console.error("Erreur lors de la récupération de l'utilisateur:", err);
                const errorMessage = err instanceof Error ? err.message : "Erreur lors de la récupération de l'utilisateur";
                setError(errorMessage);
            } finally {
                setLoading(false);
            }
        };

        fetchUtilisateur();
    }, [params.id, user, authLoading, router]);

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        setSuccessMessage(null);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!utilisateur) return;

        setSaving(true);
        setError(null);
        setSuccessMessage(null);

        try {
            const updateUserFn = httpsCallable<
                { userId: string; username: string; email: string; role: string },
                UpdateUserResponse
            >(functions, "updateUser");

            const result = await updateUserFn({
                userId: utilisateur.id,
                username: formData.username,
                email: formData.email,
                role: formData.role,
            });

            setSuccessMessage(result.data.message);

            setUtilisateur({
                ...utilisateur,
                ...formData
            });
        } catch (err) {
            console.error("Erreur lors de la modification:", err);
            
            setError(
                typeof err === "object" && err !== null && "message" in err
                    ? String((err as { message?: unknown }).message)
                    : String(err)
            );
        } finally {
            setSaving(false);
        }
    };

    const handleCancel = () => {
        router.push("/utilisateurs");
    };

    if (authLoading || loading) {
        return (
            <div className="p-8">
                <div className="max-w-3xl mx-auto">
                    <div className="flex items-center justify-center min-h-100">
                        <div className="text-center">
                            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
                            <p className="mt-4 text-gray-600 dark:text-gray-400">Chargement...</p>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    if (error && !utilisateur) {
        return (
            <div className="p-8">
                <div className="max-w-3xl mx-auto">
                    <div className="bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-200 px-4 py-3 rounded mb-4">
                        <p className="font-bold">Erreur</p>
                        <p>{error}</p>
                    </div>
                    <button
                        onClick={handleCancel}
                        className="px-4 py-2 bg-gray-200 dark:bg-gray-700 text-gray-900 dark:text-white rounded-lg hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors"
                    >
                        Retour à la liste
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="p-8">
            <div className="max-w-3xl mx-auto">
                {/* En-tête */}
                <div className="mb-8">
                    <button
                        onClick={handleCancel}
                        className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 mb-4 inline-flex items-center"
                    >
                        <svg
                            className="w-5 h-5 mr-2"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M15 19l-7-7 7-7"
                            />
                        </svg>
                        Retour à la liste
                    </button>
                    <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
                        Modifier l&apos;utilisateur
                    </h1>
                    <p className="text-gray-600 dark:text-gray-400">
                        Modifiez les informations de l&apos;utilisateur
                    </p>
                </div>

                {/* Messages */}
                {successMessage && (
                    <div className="mb-4 bg-green-100 dark:bg-green-900 border border-green-400 dark:border-green-700 text-green-700 dark:text-green-200 px-4 py-3 rounded">
                        <p className="font-bold">Succès</p>
                        <p>{successMessage}</p>
                    </div>
                )}

                {error && utilisateur && (
                    <div className="mb-4 bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-200 px-4 py-3 rounded">
                        <p className="font-bold">Erreur</p>
                        <p>{error}</p>
                    </div>
                )}

                {/* Formulaire */}
                <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
                    <form onSubmit={handleSubmit}>
                        {/* Username */}
                        <div className="mb-6">
                            <label
                                htmlFor="username"
                                className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                            >
                                Nom d&apos;utilisateur
                            </label>
                            <input
                                type="text"
                                id="username"
                                name="username"
                                value={formData.username}
                                onChange={handleInputChange}
                                required
                                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
                            />
                        </div>

                        {/* Email */}
                        <div className="mb-6">
                            <label
                                htmlFor="email"
                                className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                            >
                                Email
                            </label>
                            <input
                                type="email"
                                id="email"
                                name="email"
                                value={formData.email}
                                onChange={handleInputChange}
                                required
                                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
                            />
                        </div>

                        {/* Role */}
                        <div className="mb-6">
                            <label
                                htmlFor="role"
                                className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                            >
                                Rôle
                            </label>
                            <select
                                id="role"
                                name="role"
                                value={formData.role}
                                onChange={handleInputChange}
                                required
                                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
                            >
                                <option value="CLIENT">Client</option>
                                <option value="FIGHTER">Fighter</option>
                                <option value="ADMIN">Admin</option>
                            </select>
                        </div>

                        {/* Info création */}
                        {utilisateur && (
                            <div className="mb-6 p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
                                <p className="text-sm text-gray-600 dark:text-gray-400">
                                    <span className="font-medium">ID:</span> {utilisateur.id}
                                </p>
                                <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                                    <span className="font-medium">Date de création:</span>{" "}
                                    {formatDateTime(utilisateur.createdAt)}
                                </p>
                            </div>
                        )}

                        {/* Boutons */}
                        <div className="flex gap-4">
                            <button
                                type="submit"
                                disabled={saving}
                                className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
                            >
                                {saving ? (
                                    <>
                                        <svg
                                            className="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
                                            xmlns="http://www.w3.org/2000/svg"
                                            fill="none"
                                            viewBox="0 0 24 24"
                                        >
                                            <circle
                                                className="opacity-25"
                                                cx="12"
                                                cy="12"
                                                r="10"
                                                stroke="currentColor"
                                                strokeWidth="4"
                                            ></circle>
                                            <path
                                                className="opacity-75"
                                                fill="currentColor"
                                                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                                            ></path>
                                        </svg>
                                        Enregistrement...
                                    </>
                                ) : (
                                    "Enregistrer les modifications"
                                )}
                            </button>
                            <button
                                type="button"
                                onClick={handleCancel}
                                disabled={saving}
                                className="px-6 py-2 bg-gray-200 dark:bg-gray-700 text-gray-900 dark:text-white rounded-lg hover:bg-gray-300 dark:hover:bg-gray-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Annuler
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
}
