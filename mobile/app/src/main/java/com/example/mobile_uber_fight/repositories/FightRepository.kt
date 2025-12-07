package com.example.mobile_uber_fight.repositories

import com.example.mobile_uber_fight.models.Fight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
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
}
