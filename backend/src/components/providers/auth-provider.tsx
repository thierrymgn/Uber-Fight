import React from "react";

export const AuthContext = React.createContext<{
    user: null | { name: string };
    login: (name: string) => void;
    logout: () => void;
}>({
    user: null,
    login: () => {},
    logout: () => {},
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [user, setUser] = React.useState<null | { name: string }>(null);

    const login = (name: string) => {
        setUser({ name });
    };

    const logout = () => {
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
}