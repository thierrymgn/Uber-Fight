package com.example.mobile_uber_fight.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

object RoleManager {
    enum class UserRole {
        FIGHTER,
        CLIENT,
        UNKNOWN
    }

    fun routeUser(
        onSuccess: (UserRole) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onFailure(Exception("Utilisateur non connecté."))
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val roleString = document.getString("role")
                val role = when (roleString) {
                    "FIGHTER" -> UserRole.FIGHTER
                    "CLIENT" -> UserRole.CLIENT
                    else -> UserRole.UNKNOWN
                }
                onSuccess(role)
            }
            .addOnFailureListener { exception ->
                Log.e("RoleManager", "Erreur lors de la récupération du rôle", exception)
                onFailure(exception)
            }
    }
}
