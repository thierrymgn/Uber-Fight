"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { doc, getDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/auth-provider";
import { User } from "@/types/user";
import { formatDateTime } from "@/lib/composables";
import UserRoleTag from "@/app/utilisateurs/components/user-role-tag";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Alert, AlertTitle, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import {
    AlertCircle,
    ArrowLeft,
    Calendar,
    Mail,
    MapPin,
    Pencil,
    Smartphone,
    Star,
    User as UserIcon,
} from "lucide-react";

export default function ViewUserPage() {
    const params = useParams();
    const router = useRouter();
    const { user, loading: authLoading } = useAuth();
    const [userData, setUserData] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (authLoading) return;

        if (!user) {
            router.push("/login");
            return;
        }

        const fetchUserData = async () => {
            try {
                setLoading(true);

                // Vérification du rôle admin de l'utilisateur connecté
                const currentUserDoc = await getDoc(doc(db, "users", user.uid));
                const currentUserData = currentUserDoc.data();

                if (!currentUserDoc.exists() || currentUserData?.role?.toUpperCase() !== "ADMIN") {
                    setError("Accès refusé. Seuls les administrateurs peuvent voir les profils utilisateurs.");
                    setLoading(false);
                    return;
                }

                const userId = params.id as string;
                const docSnap = await getDoc(doc(db, "users", userId));

                if (docSnap.exists()) {
                    setUserData({
                        id: docSnap.id,
                        ...docSnap.data(),
                    } as User);
                    setError(null);
                } else {
                    setError("Utilisateur non trouvé");
                }
            } catch (err) {
                console.error("Erreur lors de la récupération de l'utilisateur:", err);
                setError(
                    err instanceof Error ? err.message : "Erreur lors de la récupération de l'utilisateur"
                );
            } finally {
                setLoading(false);
            }
        };

        fetchUserData();
    }, [params.id, user, authLoading, router]);

    const getUserInitials = (username: string) => {
        return username
            .split(" ")
            .map((n) => n[0])
            .join("")
            .toUpperCase()
            .slice(0, 2);
    };

    if (authLoading || loading) {
        return (
            <div className="p-6 space-y-6">
                <div className="flex items-center gap-4">
                    <Skeleton className="h-16 w-16 rounded-full" />
                    <div>
                        <Skeleton className="h-8 w-48 mb-2" />
                        <Skeleton className="h-4 w-32" />
                    </div>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {[1, 2, 3].map((i) => (
                        <Card key={i}>
                            <CardContent className="p-6 space-y-4">
                                <Skeleton className="h-4 w-32" />
                                <Skeleton className="h-4 w-full" />
                                <Skeleton className="h-4 w-full" />
                            </CardContent>
                        </Card>
                    ))}
                </div>
            </div>
        );
    }

    if (error || !userData) {
        return (
            <div className="p-6 space-y-4">
                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertTitle>Erreur</AlertTitle>
                    <AlertDescription>{error ?? "Utilisateur non trouvé"}</AlertDescription>
                </Alert>
                <Button variant="outline" onClick={() => router.push("/utilisateurs")}>
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Retour à la liste
                </Button>
            </div>
        );
    }

    return (
        <div className="p-6 space-y-6">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div className="flex items-center gap-4">
                    <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => router.push("/utilisateurs")}
                    >
                        <ArrowLeft className="h-4 w-4" />
                    </Button>
                    <Avatar className="h-16 w-16">
                        <AvatarImage src={userData.photoUrl || undefined} />
                        <AvatarFallback className="text-lg">
                            {getUserInitials(userData.username)}
                        </AvatarFallback>
                    </Avatar>
                    <div>
                        <div className="flex items-center gap-3">
                            <h1 className="text-3xl font-bold tracking-tight">
                                {userData.username}
                            </h1>
                            <UserRoleTag role={userData.role} />
                        </div>
                        <p className="text-muted-foreground">{userData.email}</p>
                    </div>
                </div>
                <Button
                    onClick={() => router.push(`/utilisateurs/${userData.id}/edit`)}
                >
                    <Pencil className="h-4 w-4 mr-2" />
                    Modifier
                </Button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Informations générales */}
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <UserIcon className="h-4 w-4" />
                            Informations générales
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="flex justify-between">
                            <span className="text-sm text-muted-foreground">Nom d&apos;utilisateur</span>
                            <span className="text-sm font-medium">{userData.username}</span>
                        </div>
                        <Separator />
                        <div className="flex justify-between">
                            <span className="text-sm text-muted-foreground flex items-center gap-2">
                                <Mail className="h-3.5 w-3.5" />
                                Email
                            </span>
                            <span className="text-sm font-medium">{userData.email}</span>
                        </div>
                        <Separator />
                        <div className="flex justify-between items-center">
                            <span className="text-sm text-muted-foreground">Rôle</span>
                            <UserRoleTag role={userData.role} />
                        </div>
                        <Separator />
                        <div className="flex justify-between">
                            <span className="text-sm text-muted-foreground flex items-center gap-2">
                                <Calendar className="h-3.5 w-3.5" />
                                Date de création
                            </span>
                            <span className="text-sm font-medium">
                                {formatDateTime(userData.createdAt)}
                            </span>
                        </div>
                    </CardContent>
                </Card>

                {/* Profil */}
                <Card>
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Star className="h-4 w-4" />
                            Profil
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="flex justify-between items-center">
                            <span className="text-sm text-muted-foreground">Note moyenne</span>
                            <div className="flex items-center gap-1.5">
                                <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                                <span className="text-sm font-medium">
                                    {userData.rating.toFixed(1)}
                                </span>
                                <span className="text-xs text-muted-foreground">
                                    ({userData.ratingCount} avis)
                                </span>
                            </div>
                        </div>
                        <Separator />
                        <div className="flex justify-between items-center">
                            <span className="text-sm text-muted-foreground">Photo de profil</span>
                            <span className="text-sm font-medium">
                                {userData.photoUrl ? "Définie" : "Non définie"}
                            </span>
                        </div>
                        <Separator />
                        <div className="flex justify-between items-center">
                            <span className="text-sm text-muted-foreground flex items-center gap-2">
                                <MapPin className="h-3.5 w-3.5" />
                                Localisation
                            </span>
                            <span className="text-sm font-medium">
                                {userData.location
                                    ? `${userData.location.latitude.toFixed(4)}, ${userData.location.longitude.toFixed(4)}`
                                    : "Non renseignée"}
                            </span>
                        </div>
                    </CardContent>
                </Card>

                {/* Informations techniques */}
                <Card className="md:col-span-2">
                    <CardHeader>
                        <CardTitle className="flex items-center gap-2">
                            <Smartphone className="h-4 w-4" />
                            Informations techniques
                        </CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                        <div className="flex justify-between">
                            <span className="text-sm text-muted-foreground">UID</span>
                            <span className="text-sm font-mono font-medium">{userData.id}</span>
                        </div>
                        <Separator />
                        <div className="flex justify-between">
                            <span className="text-sm text-muted-foreground">FCM Token</span>
                            <span className="text-sm font-mono font-medium">
                                {userData.fcmToken
                                    ? `${userData.fcmToken.slice(0, 24)}...`
                                    : "Non enregistré"}
                            </span>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    );
}
