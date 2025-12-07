package com.example.mobile_uber_fight.repositories

import com.example.mobile_uber_fight.models.Fight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.Date

class FightRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
            fighterId = null,
            status = "PENDING",
            fightType = "UNKNOWN_OPPONENT",
            location = GeoPoint(lat, lng),
            address = address,
            scheduledTime = Date()
        )

        db.collection("fights")
            .add(newFight)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}