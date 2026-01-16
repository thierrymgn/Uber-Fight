package com.example.mobile_uber_fight.repositories

import com.example.mobile_uber_fight.models.UserSettings
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SettingsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    fun getUserSettings(): Task<UserSettings?> {
        val uid = currentUserId ?: return com.google.android.gms.tasks.Tasks.forException(Exception("User not logged in"))
        return db.collection("userSettings").document(uid).get().continueWith { task ->
            if (task.isSuccessful && task.result.exists()) {
                task.result.toObject(UserSettings::class.java)
            } else {
                UserSettings()
            }
        }
    }

    fun updateUserSettings(settings: UserSettings): Task<Void> {
        val uid = currentUserId ?: return com.google.android.gms.tasks.Tasks.forException(Exception("User not logged in"))
        return db.collection("userSettings").document(uid).set(settings, SetOptions.merge())
    }

    fun updateNotificationPreference(enabled: Boolean): Task<Void> {
        val uid = currentUserId ?: return com.google.android.gms.tasks.Tasks.forException(Exception("User not logged in"))
        return db.collection("userSettings").document(uid).set(mapOf("notificationsEnabled" to enabled), SetOptions.merge())
    }

    fun updateDarkModePreference(enabled: Boolean): Task<Void> {
        val uid = currentUserId ?: return com.google.android.gms.tasks.Tasks.forException(Exception("User not logged in"))
        return db.collection("userSettings").document(uid).set(mapOf("darkModeEnabled" to enabled), SetOptions.merge())
    }
}
