package com.example.mobile_uber_fight.repositories

import com.example.mobile_uber_fight.models.User
import com.example.mobile_uber_fight.models.UserSettings
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun register(email: String, pass: String, username: String, role: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, pass).onSuccessTask { authResult ->
            val firebaseUser = authResult.user
            val uid = firebaseUser?.uid ?: throw IllegalStateException("User UID is null")

            val newUser = User(
                uid = uid,
                username = username,
                email = email,
                role = role
            )

            val defaultSettings = UserSettings()

            val userTask = db.collection("users").document(uid).set(newUser)
            val settingsTask = db.collection("userSettings").document(uid).set(defaultSettings)

            Tasks.whenAll(userTask, settingsTask).continueWithTask { task ->
                if (task.isSuccessful) {
                    Tasks.forResult(authResult)
                } else {
                    Tasks.forException(task.exception ?: Exception("Error creating user data"))
                }
            }
        }
    }

    fun sendPasswordResetEmail(email: String): Task<Void> {
        return auth.sendPasswordResetEmail(email)
    }

    fun updatePassword(oldPass: String, newPass: String): Task<Void> {
        val user = auth.currentUser ?: return Tasks.forException(Exception("Utilisateur non connecté"))
        val email = user.email ?: return Tasks.forException(Exception("Email utilisateur introuvable"))

        // Re-authenticate user before updating password
        val credential = EmailAuthProvider.getCredential(email, oldPass)

        return user.reauthenticate(credential).onSuccessTask {
            user.updatePassword(newPass)
        }
    }
}
