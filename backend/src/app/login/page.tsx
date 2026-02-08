"use client";

import { useState, FormEvent } from "react";
import { useAuth } from "@/components/providers/auth-provider";
import { useRouter, useSearchParams } from "next/navigation";
import Image from "next/image";
import { AuthError } from "firebase/auth";
import { doc, getDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [resetEmailSent, setResetEmailSent] = useState(false);
    const [showResetPassword, setShowResetPassword] = useState(false);

    const { login, logout, resetPassword } = useAuth();
    const router = useRouter();
    const searchParams = useSearchParams();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            const userCredential = await login(email, password);
            
            const userDoc = await getDoc(doc(db, "users", userCredential.user.uid));
            const userData = userDoc.data();
            
            if (!userDoc.exists() || userData?.role?.toUpperCase() !== "ADMIN") {
                try {
                    await logout();
                } catch (logoutError) {
                    console.error("Failed to logout non-admin user:", logoutError);
                }
                setError("Accès refusé. Seuls les administrateurs peuvent se connecter.");
                setLoading(false);
                return;
            }
            
            const redirectTo = searchParams.get("redirect") || "/";
            router.push(redirectTo);
        } catch (err) {
            const authError = err as AuthError;
            setError(
                authError.code === "auth/invalid-credential"
                    ? "Email ou mot de passe incorrect"
                    : authError.code === "auth/invalid-email"
                    ? "Email invalide"
                    : "Une erreur est survenue. Veuillez réessayer."
            );
        } finally {
            setLoading(false);
        }
    };

    const handleResetPassword = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            await resetPassword(email);
            setResetEmailSent(true);
        } catch (err) {
            const authError = err as AuthError;
            setError(
                authError.code === "auth/invalid-email"
                    ? "Email invalide"
                    : authError.code === "auth/user-not-found"
                    ? "Aucun utilisateur trouvé avec cet email"
                    : "Une erreur est survenue. Veuillez réessayer."
            );
        } finally {
            setLoading(false);
        }
    };

    if (showResetPassword) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-gray-100 dark:bg-gray-900 px-4">
                <div className="max-w-md w-full bg-white dark:bg-gray-800 rounded-lg shadow-lg p-8">
                    <div className="flex justify-center mb-6">
                        <Image
                            src="/logo/ic_logo_uber_fight.png"
                            alt="Uber Fight Logo"
                            width={80}
                            height={80}
                        />
                    </div>
                    <h2 className="text-2xl font-bold text-center mb-6 text-gray-900 dark:text-white">
                        Réinitialiser le mot de passe
                    </h2>

                    {resetEmailSent ? (
                        <div className="text-center">
                            <p className="text-green-600 dark:text-green-400 mb-4">
                                Un email de réinitialisation a été envoyé à votre adresse.
                            </p>
                            <button
                                onClick={() => {
                                    setShowResetPassword(false);
                                    setResetEmailSent(false);
                                }}
                                className="text-blue-600 dark:text-blue-400 hover:underline"
                            >
                                Retour à la connexion
                            </button>
                        </div>
                    ) : (
                        <form onSubmit={handleResetPassword}>
                            <div className="mb-4">
                                <label
                                    htmlFor="reset-email"
                                    className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                                >
                                    Email
                                </label>
                                <input
                                    id="reset-email"
                                    type="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                    className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
                                    placeholder="votre.email@exemple.com"
                                />
                            </div>

                            {error && (
                                <div className="mb-4 p-3 bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-200 rounded">
                                    {error}
                                </div>
                            )}

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {loading ? "Envoi..." : "Envoyer l'email de réinitialisation"}
                            </button>

                            <div className="mt-4 text-center">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setShowResetPassword(false);
                                        setError("");
                                    }}
                                    className="text-sm text-blue-600 dark:text-blue-400 hover:underline"
                                >
                                    Retour à la connexion
                                </button>
                            </div>
                        </form>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 dark:bg-gray-900 px-4">
            <div className="max-w-md w-full bg-white dark:bg-gray-800 rounded-lg shadow-lg p-8">
                <div className="flex justify-center mb-6">
                    <Image
                        src="/logo/ic_logo_uber_fight.png"
                        alt="Uber Fight Logo"
                        width={80}
                        height={80}
                    />
                </div>
                <h2 className="text-2xl font-bold text-center mb-6 text-gray-900 dark:text-white">
                    Connexion Admin
                </h2>

                <form onSubmit={handleSubmit}>
                    <div className="mb-4">
                        <label
                            htmlFor="email"
                            className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                        >
                            Email
                        </label>
                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
                            placeholder="votre.email@exemple.com"
                        />
                    </div>

                    <div className="mb-6">
                        <label
                            htmlFor="password"
                            className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2"
                        >
                            Mot de passe
                        </label>
                        <input
                            id="password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            minLength={6}
                            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent dark:bg-gray-700 dark:text-white"
                            placeholder="••••••••"
                        />
                    </div>

                    {error && (
                        <div className="mb-4 p-3 bg-red-100 dark:bg-red-900 border border-red-400 dark:border-red-700 text-red-700 dark:text-red-200 rounded">
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-lg transition duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {loading ? "Chargement..." : "Se connecter"}
                    </button>
                </form>

                <div className="mt-4 text-center">
                    <button
                        onClick={() => {
                            setShowResetPassword(true);
                            setError("");
                        }}
                        className="text-sm text-blue-600 dark:text-blue-400 hover:underline"
                    >
                        Mot de passe oublié ?
                    </button>
                </div>
            </div>
        </div>
    );
}

