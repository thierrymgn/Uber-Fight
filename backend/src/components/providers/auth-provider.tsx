"use client";

import React, { createContext, useContext, useEffect, useState } from "react";
import {
    User,
    UserCredential,
    signInWithEmailAndPassword,
    signOut as firebaseSignOut,
    onAuthStateChanged,
    sendPasswordResetEmail,
} from "firebase/auth";
import { auth } from "@/lib/firebase";
import useLogger from "@/hooks/useLogger";

interface AuthContextType {
    user: User | null;
    loading: boolean;
    login: (email: string, password: string) => Promise<UserCredential>;
    logout: () => Promise<void>;
    resetPassword: (email: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
    user: null,
    loading: true,
    login: async () => { throw new Error("AuthContext not initialized"); },
    logout: async () => {},
    resetPassword: async () => {},
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);
    const { logInfo, logError } = useLogger();

    useEffect(() => {
        let unsubscribe: (() => void) | undefined;
        try {
            unsubscribe = onAuthStateChanged(
                auth,
                (user) => {
                    setUser(user);
                    setLoading(false);
                },
                (error) => {
                    logError("Error observing auth state", { error: error instanceof Error ? error.message : String(error) });
                    setUser(null);
                    setLoading(false);
                }
            );
        } catch (error) {
            logError("Failed to set up auth state listener", { error: error instanceof Error ? error.message : String(error) });
            setUser(null);
            setLoading(false);
        }
        return unsubscribe ?? (() => {});
    }, [logError]);

    const login = async (email: string, password: string): Promise<UserCredential> => {
        try {
            const userCredential = await signInWithEmailAndPassword(auth, email, password);
            logInfo("User logged in successfully", { email });
            return userCredential;
        } catch (error) {
            logError("Failed to log in user", { email, error: error instanceof Error ? error.message : String(error) });
            throw error;
        }
    };

    const logout = async () => {
        try {
            await firebaseSignOut(auth);
            logInfo("User logged out successfully");
        } catch (error) {
            logError("Failed to log out user", { error: error instanceof Error ? error.message : String(error) });
        }
    };

    const resetPassword = async (email: string) => {
        try {
            await sendPasswordResetEmail(auth, email);
            logInfo("Password reset email sent", { email });
        } catch (error) {
            logError("Failed to send password reset email", { email, error: error instanceof Error ? error.message : String(error) });
            throw error;
        }
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                loading,
                login,
                logout,
                resetPassword,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
};
