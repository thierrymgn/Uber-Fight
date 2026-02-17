package com.example.mobile_uber_fight.repositories

import com.example.mobile_uber_fight.models.Fight
import com.example.mobile_uber_fight.logger.GrafanaLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import java.util.Date

class FightRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val fightsCollection = db.collection("fights")

    fun createFightRequest(
        address: String,
        lat: Double,
        lng: Double,
        fightType: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val ex = Exception("Utilisateur non connecté")
            GrafanaLogger.logError("Create fight failed: No user", ex)
            onFailure(ex)
            return
        }

        val fightData = hashMapOf(
            "requesterId" to currentUser.uid,
            "status" to "PENDING",
            "fightType" to fightType,
            "location" to GeoPoint(lat, lng),
            "address" to address,
            "fighterId" to null,
            "createdAt" to FieldValue.serverTimestamp()
        )

        GrafanaLogger.logInfo("Creating fight request", mapOf("address" to address, "type" to fightType))

        fightsCollection.add(fightData)
            .addOnSuccessListener { documentReference ->
                GrafanaLogger.logInfo("Fight request created", mapOf("fightId" to documentReference.id))
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { e ->
                GrafanaLogger.logError("Fight request creation failed", e)
                onFailure(e)
            }
    }

    fun cancelFight(fightId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        GrafanaLogger.logInfo("Cancelling fight", mapOf("fightId" to fightId))
        fightsCollection.document(fightId)
            .update("status", "CANCELLED")
            .addOnSuccessListener {
                GrafanaLogger.logInfo("Fight cancelled successfully", mapOf("fightId" to fightId))
                onSuccess()
            }
            .addOnFailureListener { e ->
                GrafanaLogger.logError("Fight cancellation failed", e, mapOf("fightId" to fightId))
                onFailure(e)
            }
    }

    fun updateFightStatus(fightId: String, newStatus: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit = {}) {
        GrafanaLogger.logInfo("Updating fight status", mapOf("fightId" to fightId, "newStatus" to newStatus))
        fightsCollection.document(fightId)
            .update("status", newStatus)
            .addOnSuccessListener {
                GrafanaLogger.logInfo("Fight status updated", mapOf("fightId" to fightId, "status" to newStatus))
                onSuccess()
            }
            .addOnFailureListener { e ->
                GrafanaLogger.logError("Fight status update failed", e, mapOf("fightId" to fightId, "status" to newStatus))
                onFailure(e)
            }
    }

    fun listenToPendingFights(onUpdate: (List<Fight>) -> Unit, onFailure: (Exception) -> Unit) {
        fightsCollection
            .whereEqualTo("status", "PENDING")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    GrafanaLogger.logError("Listen pending fights failed", e)
                    onFailure(e)
                    return@addSnapshotListener
                }

                val fightList = snapshots!!.map { document ->
                    document.toObject(Fight::class.java).copy(id = document.id)
                }
                onUpdate(fightList)
            }
    }

    fun listenToCurrentRequest(userId: String, onUpdate: (Fight?) -> Unit) {
        fightsCollection
            .whereEqualTo("requesterId", userId)
            .whereIn("status", listOf("PENDING", "ACCEPTED", "IN_PROGRESS"))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    GrafanaLogger.logError("Listen current request failed", e, mapOf("userId" to userId))
                    onUpdate(null)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val fight = snapshots.documents.first().toObject(Fight::class.java)?.copy(
                        id = snapshots.documents.first().id
                    )
                    onUpdate(fight)
                } else {
                    onUpdate(null)
                }
            }
    }

    fun acceptFight(fightId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val fighterId = auth.currentUser?.uid
        if (fighterId == null) {
            val ex = Exception("Impossible d'accepter: utilisateur non connecté.")
            GrafanaLogger.logError("Accept fight failed: No fighter session", ex, mapOf("fightId" to fightId))
            onFailure(ex)
            return
        }

        GrafanaLogger.logInfo("Fighter accepting fight", mapOf("fightId" to fightId, "fighterId" to fighterId))

        fightsCollection.document(fightId)
            .update(
                mapOf(
                    "status" to "ACCEPTED",
                    "fighterId" to fighterId
                )
            )
            .addOnSuccessListener {
                GrafanaLogger.logInfo("Fight accepted successfully", mapOf("fightId" to fightId))
                onSuccess()
            }
            .addOnFailureListener { e ->
                GrafanaLogger.logError("Accept fight failed", e, mapOf("fightId" to fightId))
                onFailure(e)
            }
    }

    fun listenToMyActiveFight(onFightFound: (Fight?) -> Unit, onFailure: (Exception) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFightFound(null)
            return
        }

        fightsCollection
            .whereEqualTo("fighterId", currentUser.uid)
            .whereIn("status", listOf("ACCEPTED", "IN_PROGRESS"))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    GrafanaLogger.logError("Listen active fight failed", e, mapOf("fighterId" to currentUser.uid))
                    onFailure(e)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val activeFight = snapshots.documents.first().toObject(Fight::class.java)?.copy(
                        id = snapshots.documents.first().id
                    )
                    onFightFound(activeFight)
                } else {
                    onFightFound(null)
                }
            }
    }
    
    fun getClientHistory(onSuccess: (List<Fight>) -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure(Exception("Utilisateur non connecté"))
            return
        }

        fightsCollection
            .whereEqualTo("requesterId", uid)
            .whereIn("status", listOf("COMPLETED", "CANCELLED"))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                val history = snapshots.map { document ->
                    document.toObject(Fight::class.java).copy(id = document.id)
                }
                onSuccess(history)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getFighterHistory(onSuccess: (List<Fight>) -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onFailure(Exception("Utilisateur non connecté"))
            return
        }

        fightsCollection
            .whereEqualTo("fighterId", uid)
            .whereIn("status", listOf("COMPLETED", "CANCELLED"))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshots ->
                val history = snapshots.map { document ->
                    document.toObject(Fight::class.java).copy(id = document.id)
                }
                onSuccess(history)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
