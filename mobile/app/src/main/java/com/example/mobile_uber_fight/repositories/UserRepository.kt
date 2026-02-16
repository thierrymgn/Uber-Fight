package com.example.mobile_uber_fight.repositories

import android.util.Log
import com.example.mobile_uber_fight.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun updateUserLocation(latitude: Double, longitude: Double) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val location = GeoPoint(latitude, longitude)

            db.collection("users").document(uid)
                .update("location", location)
                .addOnSuccessListener {
                    Log.d("UserRepository", "User location updated successfully.")
                }
                .addOnFailureListener { e ->
                    Log.w("UserRepository", "Error updating user location", e)
                }
        } else {
            Log.w("UserRepository", "No authenticated user found.")
        }
    }

    fun listenToNearbyFighters(onFightersUpdate: (List<User>) -> Unit) {
        db.collection("users")
            .whereEqualTo("role", "FIGHTER")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserRepository", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val fighters = snapshot.toObjects(User::class.java)
                    onFightersUpdate(fighters)
                }
            }
    }

    fun listenToUserLocation(userId: String, onUpdate: (GeoPoint?) -> Unit): ListenerRegistration {
        return db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("UserRepository", "Listen to user location failed.", e)
                    return@addSnapshotListener
                }
                
                val location = snapshot?.getGeoPoint("location")
                onUpdate(location)
            }
    }

    fun submitRating(
        targetUserId: String,
        fightId: String,
        rating: Float,
        comment: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser ?: return

        val reviewData = hashMapOf(
            "fromUserId" to currentUser.uid,
            "toUserId" to targetUserId,
            "fightId" to fightId,
            "rating" to rating.toDouble(),
            "comment" to comment,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("reviews")
            .add(reviewData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getCurrentUser(onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(User::class.java)
                        onSuccess(user)
                    } else {
                        onSuccess(null)
                    }
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } else {
            onSuccess(null)
        }
    }

    fun updateUserProfile(
        username: String,
        email: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val updates = hashMapOf<String, Any>(
                "username" to username,
                "email" to email
            )

            db.collection("users").document(currentUser.uid)
                .update(updates)
                .addOnSuccessListener {
                    Log.d("UserRepository", "User profile updated successfully.")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.w("UserRepository", "Error updating user profile", e)
                    onFailure(e)
                }
        } else {
            onFailure(Exception("No authenticated user found."))
        }
    }
}