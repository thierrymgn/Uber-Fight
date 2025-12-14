package com.example.mobile_uber_fight.utils

import android.content.Context
import android.content.Intent
import com.example.mobile_uber_fight.activities.ClientHomeActivity
import com.example.mobile_uber_fight.activities.FighterHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object RoleManager {

    fun routeUser(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                val intent = when (role) {
                    "FIGHTER" -> Intent(context, FighterHomeActivity::class.java)
                    else -> Intent(context, ClientHomeActivity::class.java) // Default to CLIENT/Home
                }
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
            .addOnFailureListener {
                // Handle error, maybe default to ClientHomeActivity
                val intent = Intent(context, ClientHomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
    }
}
