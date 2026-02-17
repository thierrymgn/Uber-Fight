import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import { setGlobalOptions } from "firebase-functions/v2";
import { logFunction } from "./lib/grafana-logger";
import { pushCounter } from "./lib/grafana-metrics";

export { deleteUser, updateUser, createUser } from "./users";
export { collectBusinessMetrics } from "./scheduled-metrics";

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

    await logFunction("onFightStatusChanged", "Fight status changed", "info", {
        fightId,
        oldStatus: before?.status,
        newStatus,
        clientUserId,
        fighterUserId,
    });

    pushCounter("business.fight.status_change", 1, {
        old_status: before?.status || "unknown",
        new_status: newStatus || "unknown",
    }).catch(() => {});

    let title = "";
    let body = "";
    let targetUserId = "";
    
    let clickAction = ""; 
    let userIdToRate = "";

    if (newStatus === "PENDING" && before?.status === "ACCEPTED") {
        title = "Mission annulée";
        body = "Le bagarreur a annulé. Votre demande est de nouveau en attente.";
        targetUserId = clientUserId;
        clickAction = "OPEN_MAP";
    }
    else if (newStatus === "ACCEPTED") {
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
            await logFunction("onFightStatusChanged", "FCM notification sent", "info", {
                fightId,
                targetUserId,
                clickAction,
                newStatus,
            });
        } catch (error) {
            await logFunction("onFightStatusChanged", "FCM notification failed", "error", {
                fightId,
                targetUserId,
                errorMessage: error instanceof Error ? error.message : String(error),
            });
        }
    }
});

export const onReviewCreated = onDocumentCreated("reviews/{reviewId}", async (event) => {
    const reviewId = event.params.reviewId;
    
    await logFunction("onReviewCreated", "Review trigger started", "info", { reviewId });

    pushCounter("business.review.created", 1).catch(() => {});

    const snapshot = event.data;
    if (!snapshot) {
        await logFunction("onReviewCreated", "No data in event", "error", { reviewId });
        return;
    }

    const reviewData = snapshot.data();
    const targetUserId = reviewData.toUserId;
    const newRating = Number(reviewData.rating);

    await logFunction("onReviewCreated", "Processing review", "info", {
        reviewId,
        targetUserId,
        rating: newRating,
    });

    if (!targetUserId || newRating === undefined || newRating === null || Number.isNaN(newRating)) {
        await logFunction("onReviewCreated", "Invalid review data", "error", {
            reviewId,
            targetUserId: targetUserId || "missing",
            rating: newRating,
        });
        return;
    }

    const userRef = admin.firestore().collection("users").doc(targetUserId);

    try {
        await admin.firestore().runTransaction(async (transaction) => {
            const userDoc = await transaction.get(userRef);
            
            if (!userDoc.exists) {
                await logFunction("onReviewCreated", "User not found", "error", {
                    reviewId,
                    targetUserId,
                });
                return;
            }

            const userData = userDoc.data();
            const oldRating = Number(userData?.rating || 0);
            const oldCount = Number(userData?.ratingCount || 0);

            const newCount = oldCount + 1;
            const newAverage = oldRating + (newRating - oldRating) / newCount;

            transaction.update(userRef, {
                rating: newAverage,
                ratingCount: newCount
            });
            
            await logFunction("onReviewCreated", "Rating updated", "info", {
                reviewId,
                targetUserId,
                oldRating,
                newRating: newAverage,
                oldCount,
                newCount,
            });
        });
    } catch (error) {
        await logFunction("onReviewCreated", "Transaction failed", "error", {
            reviewId,
            targetUserId,
            errorMessage: error instanceof Error ? error.message : String(error),
        });
    }
});