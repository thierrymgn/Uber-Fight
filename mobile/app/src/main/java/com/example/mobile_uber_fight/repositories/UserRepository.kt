package com.example.mobile_uber_fight.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

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
}