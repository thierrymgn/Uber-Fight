"use client";


import {Utilisateur} from "@/types/utilisateur";
import UserRoleTag from "@/app/utilisateurs/components/user-role-tag";

export interface UsersTableProps {
    utilisateurs: Utilisateur[];
}

export default function UsersTable({utilisateurs}: UsersTableProps) {
    return (
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
    )
}
