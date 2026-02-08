import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";

admin.initializeApp();

setGlobalOptions({ region: "europe-west1" });


export const onFightStatusChanged = onDocumentUpdated("fights/{fightId}", async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (before?.status === after?.status) return;

    const newStatus = after?.status;
    const clientUserId = after?.requesterId;
    const fighterUserId = after?.fighterId;
    const fightId = event.params.fightId;

    let title = "";
    let body = "";
    let targetUserId = "";
    
    let clickAction = ""; 
    let userIdToRate = "";

    if (newStatus === "ACCEPTED") {
        title = "Combat Accepté ! 🥊";
        body = "Un bagarreur est en route vers vous.";
        targetUserId = clientUserId;
        clickAction = "OPEN_MAP";
    } 
    else if (newStatus === "IN_PROGRESS") {
        title = "Le duel commence ! 🔔";
        body = "Préparez-vous à en découdre.";
        targetUserId = clientUserId;
        clickAction = "OPEN_FULLSCREEN";
    }
    else if (newStatus === "COMPLETED") {
        title = "Duel terminé 🏆";
        body = "Touchez ici pour noter votre prestation.";
        targetUserId = clientUserId;
        
        clickAction = "OPEN_RATING";
        userIdToRate = fighterUserId;
    }

    if (!title || !targetUserId) return;

    const userDoc = await admin.firestore().collection("users").doc(targetUserId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (fcmToken) {
        const message: admin.messaging.Message = {
            token: fcmToken,
            notification: { 
                title: title, 
                body: body 
            },
            data: {
                click_action: clickAction,
                fight_id: fightId,
                user_id_to_rate: userIdToRate || "",
                notification_type: newStatus
            }
        };

        try {
            await admin.messaging().send(message);
            console.log(`🔔 Notif envoyée à ${targetUserId} avec action ${clickAction}`);
        } catch (error) {
            console.error("Erreur envoi FCM:", error);
        }
    }
});

export const onReviewCreated = onDocumentCreated("reviews/{reviewId}", async (event) => {
    console.log("🚀 Trigger déclenché ! Début du calcul de moyenne.");

    const snapshot = event.data;
    if (!snapshot) {
        console.error("❌ Pas de données dans l'événement.");
        return;
    }

    const reviewData = snapshot.data();
    const targetUserId = reviewData.toUserId;
    const newRating = Number(reviewData.rating);

    console.log(`Review reçue pour User: ${targetUserId} | Note: ${newRating}`);

    if (!targetUserId || newRating === undefined || newRating === null || Number.isNaN(newRating)) {
        console.error("❌ Données invalides (toUserId ou rating manquant).");
        return;
    }

    const userRef = admin.firestore().collection("users").doc(targetUserId);

    try {
        await admin.firestore().runTransaction(async (transaction) => {
            const userDoc = await transaction.get(userRef);
            
            if (!userDoc.exists) {
                console.error(`❌ L'utilisateur ${targetUserId} n'existe pas dans la collection 'users'.`);
                return;
            }

            const userData = userDoc.data();
            const oldRating = Number(userData?.rating || 0);
            const oldCount = Number(userData?.ratingCount || 0);

            console.log(`📊 Avant: Moyenne ${oldRating} (${oldCount} votes)`);

            const newCount = oldCount + 1;
            const newAverage = oldRating + (newRating - oldRating) / newCount;

            transaction.update(userRef, {
                rating: newAverage,
                ratingCount: newCount
            });
            
            console.log(`✅ SUCCÈS : Nouvelle moyenne ${newAverage} (${newCount} votes) enregistrée.`);
        });
    } catch (error) {
        console.error("❌ CRASH pendant la transaction :", error);
    }
});

export const deleteUser = onCall(async (request) => {
    if (!request.auth) {
        throw new HttpsError("unauthenticated", "Vous devez être connecté pour effectuer cette action.");
    }

    const callerUid = request.auth.uid;
    const userIdToDelete = request.data.userId;

    if (!userIdToDelete || typeof userIdToDelete !== "string") {
        throw new HttpsError("invalid-argument", "L'ID de l'utilisateur à supprimer est requis.");
    }

    const callerDoc = await admin.firestore().collection("users").doc(callerUid).get();
    const callerData = callerDoc.data();

    if (!callerDoc.exists || callerData?.role.toLowerCase() !== "admin") {
        throw new HttpsError("permission-denied", "Seuls les administrateurs peuvent supprimer des utilisateurs.");
    }

    if (callerUid === userIdToDelete) {
        throw new HttpsError("failed-precondition", "Vous ne pouvez pas supprimer votre propre compte.");
    }

    try {
        await admin.firestore().collection("users").doc(userIdToDelete).delete();
        console.log(`📄 Document Firestore supprimé pour l'utilisateur ${userIdToDelete}`);

        await admin.auth().deleteUser(userIdToDelete);
        console.log(`🔐 Compte Auth supprimé pour l'utilisateur ${userIdToDelete}`);

        return { success: true, message: "Utilisateur supprimé avec succès." };
    } catch (error) {
        console.error(`❌ Erreur lors de la suppression de l'utilisateur ${userIdToDelete}:`, error);

        if (error instanceof Error && error.message.includes("auth/user-not-found")) {
            return { success: true, message: "Document utilisateur supprimé (compte Auth inexistant)." };
        }

        throw new HttpsError("internal", "Une erreur est survenue lors de la suppression de l'utilisateur.");
    }
});