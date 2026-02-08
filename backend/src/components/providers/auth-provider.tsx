"use client";

import React, { createContext, useContext, useEffect, useState } from "react";
import {
    User,
    UserCredential,
    signInWithEmailAndPassword,
    createUserWithEmailAndPassword,
    signOut as firebaseSignOut,
    onAuthStateChanged,
    sendPasswordResetEmail,
} from "firebase/auth";
import { auth } from "@/lib/firebase";

interface AuthContextType {
    user: User | null;
    loading: boolean;
    login: (email: string, password: string) => Promise<UserCredential>;
    register: (email: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    resetPassword: (email: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
    user: null,
    loading: true,
    login: async () => { throw new Error("AuthContext not initialized"); },
    register: async () => {},
    logout: async () => {},
    resetPassword: async () => {},
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

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
                    console.error("Error observing auth state:", error);
                    setUser(null);
                    setLoading(false);
                }
            );
        } catch (error) {
            console.error("Failed to set up auth state listener:", error);
            setUser(null);
            setLoading(false);
        }
        return unsubscribe ?? (() => {});
    }, []);

    const login = async (email: string, password: string): Promise<UserCredential> => {
        return await signInWithEmailAndPassword(auth, email, password);
    };

    const register = async (email: string, password: string) => {
        await createUserWithEmailAndPassword(auth, email, password);
    };

    const logout = async () => {
        await firebaseSignOut(auth);
    };

    const resetPassword = async (email: string) => {
        await sendPasswordResetEmail(auth, email);
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                loading,
                login,
                register,
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
