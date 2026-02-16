"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { doc, getDoc } from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import { db, functions } from "@/lib/firebase";
import { useAuth } from "@/components/providers/auth-provider";
import { User } from "@/types/user";
import { formatDateTime } from "@/lib/composables";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Alert, AlertTitle, AlertDescription } from "@/components/ui/alert";
import { Skeleton } from "@/components/ui/skeleton";
import { AlertCircle, ArrowLeft, CheckCircle, Loader2 } from "lucide-react";

interface UpdateUserResponse {
    success: boolean;
    message: string;
}

interface FormData {
    username: string;
    email: string;
    role: string;
}

export default function EditUserPage() {
    const params = useParams();
    const router = useRouter();
    const { user, loading: authLoading } = useAuth();
    const [userData, setUserData] = useState<User | null>(null);
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

        const fetchUserData = async () => {
            try {
                setLoading(true);

                // Vérification du rôle admin de l'utilisateur connecté
                const currentUserDoc = await getDoc(doc(db, "users", user.uid));
                const currentUserData = currentUserDoc.data();

                if (!currentUserDoc.exists() || currentUserData?.role?.toUpperCase() !== "ADMIN") {
                    setError("Accès refusé. Seuls les administrateurs peuvent modifier les utilisateurs.");
                    setLoading(false);
                    return;
                }

                const userId = params.id as string;
                const docRef = doc(db, "users", userId);
                const docSnap = await getDoc(docRef);

                if (docSnap.exists()) {
                    const fetchedUser = {
                        id: docSnap.id,
                        ...docSnap.data()
                    } as User;

                    setUserData(fetchedUser);
                    setFormData({
                        username: fetchedUser.username,
                        email: fetchedUser.email,
                        role: fetchedUser.role,
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

        fetchUserData();
    }, [params.id, user, authLoading, router]);

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
        setSuccessMessage(null);
    };

    const handleRoleChange = (value: string) => {
        setFormData(prev => ({
            ...prev,
            role: value
        }));
        setSuccessMessage(null);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!userData) return;

        setSaving(true);
        setError(null);
        setSuccessMessage(null);

        try {
            const updateUserFn = httpsCallable<
                { userId: string; username: string; email: string; role: string },
                UpdateUserResponse
            >(functions, "updateUser");

            const result = await updateUserFn({
                userId: userData.id,
                username: formData.username,
                email: formData.email,
                role: formData.role,
            });

            setSuccessMessage(result.data.message);

            setUserData({
                ...userData,
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
            <div className="p-6 space-y-6">
                <div>
                    <Skeleton className="h-8 w-48 mb-2" />
                    <Skeleton className="h-4 w-80" />
                </div>
                <Card>
                    <CardContent className="p-6 space-y-6">
                        {[1, 2, 3].map((i) => (
                            <div key={i} className="space-y-2">
                                <Skeleton className="h-4 w-32" />
                                <Skeleton className="h-9 w-full" />
                            </div>
                        ))}
                        <Skeleton className="h-20 w-full" />
                        <div className="flex gap-4">
                            <Skeleton className="h-9 flex-1" />
                            <Skeleton className="h-9 w-24" />
                        </div>
                    </CardContent>
                </Card>
            </div>
        );
    }

    if (error && !userData) {
        return (
            <div className="p-6 space-y-4">
                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertTitle>Erreur</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
                <Button variant="outline" onClick={handleCancel}>
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Retour à la liste
                </Button>
            </div>
        );
    }

    return (
        <div className="p-6 space-y-6">
            {/* En-tête */}
            <div>
                <Button
                    variant="ghost"
                    size="sm"
                    onClick={handleCancel}
                    className="mb-4"
                >
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    Retour à la liste
                </Button>
                <h1 className="text-3xl font-bold tracking-tight">
                    Modifier l&apos;utilisateur
                </h1>
                <p className="text-muted-foreground">
                    Modifiez les informations de l&apos;utilisateur
                </p>
            </div>

            {/* Messages */}
            {successMessage && (
                <Alert>
                    <CheckCircle className="h-4 w-4" />
                    <AlertTitle>Succès</AlertTitle>
                    <AlertDescription>{successMessage}</AlertDescription>
                </Alert>
            )}

            {error && userData && (
                <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4" />
                    <AlertTitle>Erreur</AlertTitle>
                    <AlertDescription>{error}</AlertDescription>
                </Alert>
            )}

            {/* Formulaire */}
            <Card>
                <CardContent className="p-6">
                    <form onSubmit={handleSubmit} className="space-y-6">
                        {/* Username */}
                        <div className="space-y-2">
                            <Label htmlFor="username">Nom d&apos;utilisateur</Label>
                            <Input
                                type="text"
                                id="username"
                                name="username"
                                value={formData.username}
                                onChange={handleInputChange}
                                required
                            />
                        </div>

                        {/* Email */}
                        <div className="space-y-2">
                            <Label htmlFor="email">Email</Label>
                            <Input
                                type="email"
                                id="email"
                                name="email"
                                value={formData.email}
                                onChange={handleInputChange}
                                required
                            />
                        </div>

                        {/* Role */}
                        <div className="space-y-2">
                            <Label htmlFor="role">Rôle</Label>
                            <Select
                                value={formData.role}
                                onValueChange={handleRoleChange}
                                required
                            >
                                <SelectTrigger className="w-full">
                                    <SelectValue placeholder="Sélectionner un rôle" />
                                </SelectTrigger>
                                <SelectContent>
                                    <SelectItem value="CLIENT">Client</SelectItem>
                                    <SelectItem value="FIGHTER">Fighter</SelectItem>
                                    <SelectItem value="ADMIN">Admin</SelectItem>
                                </SelectContent>
                            </Select>
                        </div>

                        {/* Info création */}
                        {userData && (
                            <Card className="bg-muted/50">
                                <CardHeader className="pb-2">
                                    <CardTitle className="text-sm">Informations</CardTitle>
                                </CardHeader>
                                <CardContent>
                                    <CardDescription>
                                        <span className="font-medium">ID:</span> {userData.id}
                                    </CardDescription>
                                    <CardDescription className="mt-1">
                                        <span className="font-medium">Date de création:</span>{" "}
                                        {formatDateTime(userData.createdAt)}
                                    </CardDescription>
                                </CardContent>
                            </Card>
                        )}

                        {/* Boutons */}
                        <div className="flex gap-4">
                            <Button
                                type="submit"
                                disabled={saving}
                                className="flex-1"
                            >
                                {saving && (
                                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                )}
                                {saving ? "Enregistrement..." : "Enregistrer les modifications"}
                            </Button>
                            <Button
                                type="button"
                                variant="outline"
                                onClick={handleCancel}
                                disabled={saving}
                            >
                                Annuler
                            </Button>
                        </div>
                    </form>
                </CardContent>
            </Card>
        </div>
    );
}
