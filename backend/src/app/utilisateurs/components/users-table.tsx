"use client";

import { useState } from "react";
import { Utilisateur } from "@/types/utilisateur";
import UserRoleTag from "@/app/utilisateurs/components/user-role-tag";
import DeleteConfirmationModal from "@/components/delete-confirmation-modal";
import { doc, deleteDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";

export interface UsersTableProps {
    utilisateurs: Utilisateur[];
    onUserDeleted?: (userId: string) => void;
}

export default function UsersTable({ utilisateurs, onUserDeleted }: UsersTableProps) {
    const [userToDelete, setUserToDelete] = useState<Utilisateur | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState<string | null>(null);

    const handleDeleteClick = (utilisateur: Utilisateur) => {
        setUserToDelete(utilisateur);
        setDeleteError(null);
    };

    const handleCancelDelete = () => {
        setUserToDelete(null);
        setDeleteError(null);
    };

    const handleConfirmDelete = async () => {
        if (!userToDelete) return;

        setIsDeleting(true);
        setDeleteError(null);

        try {
            await deleteDoc(doc(db, "users", userToDelete.id));

            // Appeler le callback pour mettre à jour la liste
            if (onUserDeleted) {
                onUserDeleted(userToDelete.id);
            }

            setUserToDelete(null);
        } catch (error) {
            console.error("Erreur lors de la suppression:", error);
            setDeleteError(
                error instanceof Error
                    ? error.message
                    : "Une erreur est survenue lors de la suppression"
            );
        } finally {
            setIsDeleting(false);
        }
    };

    return (
        <>
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
            <div className="overflow-x-auto">
                <table className="w-full">
                    <thead className="bg-gray-50 dark:bg-gray-700">
                    <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                            username
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
                            <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-gray-100">
                                {utilisateur.username}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                                {utilisateur.email}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm">
                                <UserRoleTag role={utilisateur.role}/>
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                                {new Date(utilisateur.createdAt).toLocaleDateString("fr-FR", {
                                    year: "numeric",
                                    month: "long",
                                    day: "numeric",
                                })}
                            </td>
                            <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                                <button
                                    className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 mr-3">
                                    Modifier
                                </button>
                                <button
                                    onClick={() => handleDeleteClick(utilisateur)}
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

            {/* Message d'erreur */}
            {deleteError && (
                <div className="mt-4 bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-200 px-4 py-3 rounded">
                    <p className="font-bold">Erreur</p>
                    <p>{deleteError}</p>
                </div>
            )}

            {/* Modal de confirmation */}
            <DeleteConfirmationModal
                isOpen={userToDelete !== null}
                onClose={handleCancelDelete}
                onConfirm={handleConfirmDelete}
                title="Confirmer la suppression"
                message={`Êtes-vous sûr de vouloir supprimer l'utilisateur "${userToDelete?.username}" ? Cette action est irréversible.`}
                isDeleting={isDeleting}
            />
        </>
    );
}
