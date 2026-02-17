package com.example.mobile_uber_fight.repositories

import android.net.Uri
import android.util.Log
import com.example.mobile_uber_fight.models.User
import com.example.mobile_uber_fight.models.UserSettings
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage

class UserRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

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

    fun createUser(user: User): Task<Void> {
        return db.collection("users").document(user.uid).set(user).onSuccessTask {
            val defaultSettings = UserSettings()
            db.collection("userSettings").document(user.uid).set(defaultSettings)
        }
    }

    fun getCurrentUser(onSuccess: (User?) -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure(Exception("User not logged in"))
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                onSuccess(document.toObject(User::class.java))
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun updateUserProfile(username: String, email: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure(Exception("User not logged in"))
        db.collection("users").document(uid)
            .update(mapOf("username" to username, "email" to email))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun uploadProfilePicture(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onFailure(Exception("User not logged in"))
        val fileRef = storage.reference.child("profile_images/$uid.jpg")

        fileRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                fileRef.downloadUrl
            }
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result.toString()
                    db.collection("users").document(uid)
                        .update("photoUrl", downloadUri)
                        .addOnSuccessListener {
                            onSuccess(downloadUri)
                        }
                        .addOnFailureListener { e ->
                            onFailure(e)
                        }
                } else {
                    task.exception?.let { onFailure(it) }
                }
            }
    }
}