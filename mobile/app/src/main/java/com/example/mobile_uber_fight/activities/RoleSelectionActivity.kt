package com.example.mobile_uber_fight.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_uber_fight.databinding.ActivityRoleSelectionBinding
import com.example.mobile_uber_fight.logger.GrafanaLogger
import com.example.mobile_uber_fight.models.User
import com.example.mobile_uber_fight.repositories.UserRepository
import com.example.mobile_uber_fight.utils.RoleManager
import com.google.firebase.auth.FirebaseAuth

class RoleSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRoleSelectionBinding
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoleSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardClient.setOnClickListener {
            handleRoleSelection("CLIENT")
        }

        binding.cardFighter.setOnClickListener {
            handleRoleSelection("FIGHTER")
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, AuthentificationActivity::class.java))
        }
    }

    private fun handleRoleSelection(role: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser != null) {
            GrafanaLogger.logInfo("Creating user profile after Google login", mapOf("role" to role))
            
            val newUser = User(
                uid = currentUser.uid,
                username = currentUser.displayName ?: "Utilisateur",
                email = currentUser.email ?: "",
                role = role,
                photoUrl = currentUser.photoUrl?.toString() ?: ""
            )

            userRepository.createUser(newUser)
                .addOnSuccessListener {
                    GrafanaLogger.logInfo("User profile created successfully", mapOf("uid" to currentUser.uid))
                    navigateToHome(role)
                }
                .addOnFailureListener { e ->
                    GrafanaLogger.logError("Failed to create user profile", e)
                    Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            navigateToRegister(role)
        }
    }

    private fun navigateToHome(role: String) {
        val intent = when (role) {
            "FIGHTER" -> Intent(this, FighterMainActivity::class.java)
            else -> Intent(this, ClientMainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToRegister(role: String) {
        val intent = Intent(this, RegisterActivity::class.java).apply {
            putExtra("ROLE", role)
        }
        startActivity(intent)
    }
}
