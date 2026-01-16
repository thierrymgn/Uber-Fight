"use client";

import { useState, FormEvent } from "react";
import { useAuth } from "@/components/providers/auth-provider";
import { useRouter } from "next/navigation";
import Image from "next/image";

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [isRegister, setIsRegister] = useState(false);
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [resetEmailSent, setResetEmailSent] = useState(false);
    const [showResetPassword, setShowResetPassword] = useState(false);

    const { login, register, resetPassword } = useAuth();
    const router = useRouter();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError("");
        setLoading(true);

        try {
            if (isRegister) {
                await register(email, password);
            } else {
                await login(email, password);
            }
            router.push("/");
        } catch (err: any) {
            setError(
                err.code === "auth/invalid-credential"
                    ? "Email ou mot de passe incorrect"
                    : err.code === "auth/email-already-in-use"
                    ? "Cet email est déjà utilisé"
                    : err.code === "auth/weak-password"
                    ? "Le mot de passe doit contenir au moins 6 caractères"
                    : err.code === "auth/invalid-email"
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
        } catch (err: any) {
            setError(
                err.code === "auth/invalid-email"
                    ? "Email invalide"
                    : err.code === "auth/user-not-found"
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
                    {isRegister ? "Créer un compte" : "Connexion"}
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
                        {loading
                            ? "Chargement..."
                            : isRegister
                            ? "Créer un compte"
                            : "Se connecter"}
                    </button>
                </form>

                {!isRegister && (
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
                )}

                <div className="mt-6 text-center">
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                        {isRegister ? "Vous avez déjà un compte ?" : "Vous n'avez pas de compte ?"}
                        <button
                            onClick={() => {
                                setIsRegister(!isRegister);
                                setError("");
                            }}
                            className="ml-2 text-blue-600 dark:text-blue-400 hover:underline font-semibold"
                        >
                            {isRegister ? "Se connecter" : "Créer un compte"}
                        </button>
                    </p>
                </div>
            </div>
        </div>
    );
}

