package com.example.mobile_uber_fight.repositories

import com.example.mobile_uber_fight.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun register(email: String, pass: String, username: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, pass).onSuccessTask { result ->
            val firebaseUser = result.user
            val uid = firebaseUser?.uid!!

            val newUser = User(
                uid = uid,
                username = username,
                email = email,
            )

            db.collection("users").document(uid).set(newUser).continueWith { task ->
                if (task.isSuccessful) {
                    result
                } else {
                    throw task.exception!!
                }
            }
        }
    }
}
