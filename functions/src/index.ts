import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";

admin.initializeApp();

setGlobalOptions({ region: "europe-west1" });

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

    if (!targetUserId || !newRating) {
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
            const newAverage = ((oldRating * oldCount) + newRating) / newCount;

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