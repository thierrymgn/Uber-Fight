package com.example.mobile_uber_fight.repositories

import com.example.mobile_uber_fight.models.User
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun register(email: String, pass: String, username: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, pass).onSuccessTask { authResult ->
            val firebaseUser = authResult.user
            val uid = firebaseUser?.uid ?: throw IllegalStateException("User UID is null")

            val newUser = User(
                uid = uid,
                username = username,
                email = email,
            )

            val firestoreTask = db.collection("users").document(uid).set(newUser)

            firestoreTask.continueWithTask { task ->
                if (task.isSuccessful) {
                    Tasks.forResult(authResult)
                } else {
                    Tasks.forException(task.exception ?: Exception("Unknown error occurred while creating user document"))
                }
            }
        }
    }
}
