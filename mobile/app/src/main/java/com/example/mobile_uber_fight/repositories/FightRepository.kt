package com.example.mobile_uber_fight.repositories

import com.example.mobile_uber_fight.models.Fight
import com.google.firebase.auth.FirebaseAuth
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
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure(Exception("Utilisateur non connecté"))
            return
        }

        val newFight = Fight(
            requesterId = currentUser.uid,
            status = "PENDING",
            fightType = fightType,
            location = GeoPoint(lat, lng),
            address = address
        )

        fightsCollection.add(newFight)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun listenToPendingFights(onUpdate: (List<Fight>) -> Unit, onFailure: (Exception) -> Unit) {
        fightsCollection
            .whereEqualTo("status", "PENDING")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
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
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
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
            onFailure(Exception("Impossible d'accepter: utilisateur non connecté."))
            return
        }

        fightsCollection.document(fightId)
            .update(
                mapOf(
                    "status" to "ACCEPTED",
                    "fighterId" to fighterId
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun finishFight(fightId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        fightsCollection.document(fightId)
            .update("status", "COMPLETED")
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
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
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
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
}
