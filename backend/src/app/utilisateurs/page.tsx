"use client";

import { useEffect, useState, useMemo } from "react";
import InfoSection from "@/app/utilisateurs/components/info-section";
import { collection, getDocs } from "@firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/auth-provider";
import { User, parseUser, isValidUser } from "@/types/user";
import UsersTable from "@/app/utilisateurs/components/users-table";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { AlertCircle, Search, UserPlus, RefreshCw } from "lucide-react";

export default function UtilisateursPage() {
    const { user, loading: authLoading } = useAuth();
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState("");
    const [roleFilter, setRoleFilter] = useState<string>("ALL");

    const fetchUsers = async () => {
        try {
            setLoading(true);
            const snapshot = await getDocs(collection(db, "users"));
            const users = snapshot.docs
                .filter(doc => isValidUser(doc.data()))
                .map(doc => parseUser(doc.id, doc.data()));

            setUsers(users);
            setError(null);
        } catch (err) {
            console.error("Erreur lors de la récupération des utilisateurs:", err);
            const errorMessage = err instanceof Error ? err.message : "Erreur lors de la récupération des utilisateurs";
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (authLoading) return;

        if (!user) {
            setError("Vous devez être connecté pour voir cette page");
            setLoading(false);
            return;
        }

        fetchUsers();
    }, [user, authLoading]);

    const filteredUsers = useMemo(() => {
        return users.filter((u) => {
            const matchesSearch =
                u.username.toLowerCase().includes(searchQuery.toLowerCase()) ||
                u.email.toLowerCase().includes(searchQuery.toLowerCase());
            const matchesRole = roleFilter === "ALL" || u.role === roleFilter;
            return matchesSearch && matchesRole;
        });
    }, [users, searchQuery, roleFilter]);

    const handleUserDeleted = (userId: string) => {
        setUsers(prevUsers =>
            prevUsers.filter(u => u.id !== userId)
        );
    };

    if (authLoading || loading) {
        return (
            <div className="p-6 space-y-6">
                <div className="flex items-center justify-between">
                    <div>
                        <Skeleton className="h-8 w-64 mb-2" />
                        <Skeleton className="h-4 w-96" />
                    </div>
                    <Skeleton className="h-10 w-40" />
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                    {[1, 2, 3, 4].map((i) => (
                        <Card key={i}>
                            <CardContent className="p-6">
                                <Skeleton className="h-4 w-24 mb-2" />
                                <Skeleton className="h-8 w-16" />
                            </CardContent>
                        </Card>
                    ))}
                </div>

                <Card>
                    <CardContent className="p-6">
                        <div className="space-y-4">
                            {[1, 2, 3, 4, 5].map((i) => (
                                <Skeleton key={i} className="h-12 w-full" />
                            ))}
                        </div>
                    </CardContent>
                </Card>
            </div>
        );
    }

    if (error) {
        return (
            <div className="p-6">
                <Card className="border-destructive">
                    <CardContent className="p-6">
                        <div className="flex items-center gap-3">
                            <AlertCircle className="h-5 w-5 text-destructive" />
                            <div>
                                <p className="font-semibold text-destructive">Erreur</p>
                                <p className="text-sm text-muted-foreground">{error}</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="p-6 space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight">
                        Gestion des utilisateurs
                    </h1>
                    <p className="text-muted-foreground">
                        {filteredUsers.length} user{filteredUsers.length > 1 ? "s" : ""} 
                        {searchQuery || roleFilter !== "ALL" ? " trouvé(s)" : " au total"}
                    </p>
                </div>
                <div className="flex gap-2">
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={fetchUsers}
                        disabled={loading}
                    >
                        <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                        Actualiser
                    </Button>
                    <Button size="sm">
                        <UserPlus className="h-4 w-4 mr-2" />
                        Ajouter
                    </Button>
                </div>
            </div>

            {/* Stats Cards */}
            <InfoSection users={users} />

            {/* Filtres */}
            <Card>
                <CardContent className="p-4">
                    <div className="flex flex-col sm:flex-row gap-4">
                        <div className="relative flex-1">
                            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                            <Input
                                placeholder="Rechercher par nom ou email..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="pl-9"
                            />
                        </div>
                        <div className="flex gap-2">
                            {["ALL", "ADMIN", "FIGHTER", "CLIENT"].map((role) => (
                                <Button
                                    key={role}
                                    variant={roleFilter === role ? "default" : "outline"}
                                    size="sm"
                                    onClick={() => setRoleFilter(role)}
                                >
                                    {role === "ALL" ? "Tous" : role}
                                </Button>
                            ))}
                        </div>
                    </div>
                </CardContent>
            </Card>

            {/* Table */}
            <UsersTable
                users={filteredUsers}
                onUserDeleted={handleUserDeleted}
            />
        </div>
    );
}

