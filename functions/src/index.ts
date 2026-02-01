import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";

admin.initializeApp();

setGlobalOptions({ region: "us-central1" });

export const onReviewCreated = onDocumentCreated("reviews/{reviewId}", async (event) => {
    const snapshot = event.data;
    
    if (!snapshot) {
        console.error("Pas de données associées à l'événement");
        return;
    }

    const reviewData = snapshot.data();

    if (!reviewData || !reviewData.toUserId || !reviewData.rating) {
        console.error("Review invalide ou incomplète");
        return;
    }

    const targetUserId = reviewData.toUserId;
    const newRating = Number(reviewData.rating);

    const userRef = admin.firestore().collection("users").doc(targetUserId);

    try {
        await admin.firestore().runTransaction(async (transaction) => {
            const userDoc = await transaction.get(userRef);

            if (!userDoc.exists) {
                throw new Error("L'utilisateur noté n'existe pas !");
            }

            const userData = userDoc.data();
            
            const oldRating = Number(userData?.rating || 0);
            const oldCount = Number(userData?.ratingCount || 0);

            const newCount = oldCount + 1;
            const newAverage = ((oldRating * oldCount) + newRating) / newCount;

            transaction.update(userRef, {
                rating: newAverage,
                ratingCount: newCount
            });

            console.log(`User ${targetUserId} updated: New Rating ${newAverage.toFixed(2)} (${newCount} votes)`);
        });
    } catch (error) {
        console.error("Erreur lors de la transaction :", error);
    }
});