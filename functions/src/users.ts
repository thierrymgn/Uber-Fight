import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { logFunction } from "./lib/grafana-logger";

const REGION = "europe-west1";

export const deleteUser = onCall({ region: REGION }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Vous devez être connecté pour effectuer cette action.");
    }

    const callerUid = request.auth.uid;
    const userIdToDelete = request.data.userId;

    await logFunction("deleteUser", "Delete user requested", "info", {
        callerUid,
        userIdToDelete,
    });

    if (!userIdToDelete || typeof userIdToDelete !== "string") {
        throw new HttpsError("invalid-argument", "L'ID de l'utilisateur à supprimer est requis.");
    }

    const callerDoc = await admin.firestore().collection("users").doc(callerUid).get();
    const callerData = callerDoc.data();

    if (!callerDoc.exists || callerData?.role.toLowerCase() !== "admin") {
        await logFunction("deleteUser", "Permission denied", "warn", {
            callerUid,
            userIdToDelete,
            reason: "not_admin",
        });
        throw new HttpsError("permission-denied", "Seuls les administrateurs peuvent supprimer des utilisateurs.");
    }

    if (callerUid === userIdToDelete) {
        throw new HttpsError("failed-precondition", "Vous ne pouvez pas supprimer votre propre compte.");
    }

    try {
        await admin.firestore().collection("users").doc(userIdToDelete).delete();
        await admin.auth().deleteUser(userIdToDelete);

        await logFunction("deleteUser", "User deleted successfully", "info", {
            callerUid,
            userIdToDelete,
        });

        return { success: true, message: "Utilisateur supprimé avec succès." };
    } catch (error) {
        await logFunction("deleteUser", "Delete user failed", "error", {
            callerUid,
            userIdToDelete,
            errorMessage: error instanceof Error ? error.message : String(error),
        });

        if (error instanceof Error && error.message.includes("auth/user-not-found")) {
            return { success: true, message: "Document utilisateur supprimé (compte Auth inexistant)." };
        }

        throw new HttpsError("internal", "Une erreur est survenue lors de la suppression de l'utilisateur.");
    }
});

export const updateUser = onCall({ region: REGION }, async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Vous devez être connecté pour effectuer cette action.");
    }

    const callerUid = request.auth.uid;
    const { userId, username, email, role } = request.data;

    await logFunction("updateUser", "Update user requested", "info", {
        callerUid,
        userId,
        fieldsToUpdate: [username ? "username" : null, email ? "email" : null, role ? "role" : null].filter(Boolean).join(", "),
    });

    if (!userId || typeof userId !== "string") {
        throw new HttpsError("invalid-argument", "L'ID de l'utilisateur est requis.");
    }

    const callerDoc = await admin.firestore().collection("users").doc(callerUid).get();
    const callerData = callerDoc.data();

    if (!callerDoc.exists || callerData?.role.toLowerCase() !== "admin") {
        await logFunction("updateUser", "Permission denied", "warn", {
            callerUid,
            userId,
            reason: "not_admin",
        });
        throw new HttpsError("permission-denied", "Seuls les administrateurs peuvent modifier des utilisateurs.");
    }

    try {
        const userDoc = await admin.firestore().collection("users").doc(userId).get();
        if (!userDoc.exists) {
            throw new HttpsError("not-found", "Utilisateur non trouvé.");
        }

        const currentUserData = userDoc.data();
        const emailChanged = email && email !== currentUserData?.email;

        const firestoreUpdate: Record<string, string> = {};
        if (username) firestoreUpdate.username = username;
        if (role) firestoreUpdate.role = role;
        if (email) firestoreUpdate.email = email;

        if (emailChanged) {
            try {
                await admin.auth().updateUser(userId, { email });
            } catch (authError) {
                await logFunction("updateUser", "Email update failed", "error", {
                    callerUid,
                    userId,
                    errorMessage: authError instanceof Error ? authError.message : String(authError),
                });
                
                if (authError instanceof Error) {
                    if (authError.message.includes("email-already-exists")) {
                        throw new HttpsError("already-exists", "Cet email est déjà utilisé par un autre compte.");
                    }
                    if (authError.message.includes("invalid-email")) {
                        throw new HttpsError("invalid-argument", "L'adresse email est invalide.");
                    }
                }
                throw new HttpsError("internal", "Erreur lors de la mise à jour de l'email.");
            }
        }

        if (Object.keys(firestoreUpdate).length > 0) {
            await admin.firestore().collection("users").doc(userId).update(firestoreUpdate);
        }

        await logFunction("updateUser", "User updated successfully", "info", {
            callerUid,
            userId,
            emailChanged,
            fieldsUpdated: Object.keys(firestoreUpdate).join(", "),
        });

        return { 
            success: true, 
            message: emailChanged 
                ? "Utilisateur modifié avec succès (email et profil mis à jour)." 
                : "Utilisateur modifié avec succès."
        };
    } catch (error) {
        if (error instanceof HttpsError) {
            throw error;
        }
        await logFunction("updateUser", "Update user failed", "error", {
            callerUid,
            userId,
            errorMessage: error instanceof Error ? error.message : String(error),
        });
        throw new HttpsError("internal", "Une erreur est survenue lors de la modification de l'utilisateur.");
    }
});
